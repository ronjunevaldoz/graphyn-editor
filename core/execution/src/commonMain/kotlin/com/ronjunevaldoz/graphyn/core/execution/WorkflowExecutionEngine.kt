package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.TimeSource

class WorkflowExecutionException(message: String) : IllegalStateException(message)

private data class NodeOutcome(
    val nodeId: String,
    val output: Map<String, WorkflowValue>?,
    val subResult: WorkflowExecutionResult?,
    val error: String?,
    val durationMs: Long,
)

/**
 * Runs a [WorkflowDefinition] layer by layer in topological order.
 *
 * Nodes within the same layer have no data dependency on each other and execute concurrently
 * via [coroutineScope]. A node that throws is recorded as an error and its transitive dependents
 * are skipped, but independent branches keep running. Pass [onEvent] to observe progress live.
 */
class WorkflowExecutionEngine(
    private val nodeExecutors: NodeExecutorRegistry,
    private val nodeSpecs: NodeSpecRegistry? = null,
) {
    suspend fun execute(
        workflow: WorkflowDefinition,
        onEvent: ((ExecutionEvent) -> Unit)? = null,
        externalInputs: Map<String, WorkflowValue> = emptyMap(),
    ): WorkflowExecutionResult {
        val nodesById = workflow.nodes.associateBy { it.id }
        if (nodesById.size != workflow.nodes.size)
            throw WorkflowExecutionException("Workflow contains duplicate node ids.")

        val layers = workflow.topologicalLayers()
        val dependents = workflow.directDependents()

        val outputs = linkedMapOf<String, Map<String, WorkflowValue>>()
        val status = linkedMapOf<String, NodeExecutionStatus>()
        val errors = linkedMapOf<String, String>()
        val durations = linkedMapOf<String, Long>()
        val subResults = linkedMapOf<String, WorkflowExecutionResult>()
        val executed = mutableListOf<String>()

        for (layer in layers) {
            val outcomes = coroutineScope {
                layer.map { nodeId ->
                    async {
                        if (status[nodeId] == NodeExecutionStatus.Skipped) return@async null
                        val node = nodesById.getValue(nodeId)
                        onEvent?.invoke(ExecutionEvent.Started(nodeId))
                        val start = TimeSource.Monotonic.markNow()
                        try {
                            val (out, sub) = runWithPolicy(node) { runNode(workflow, node, outputs, externalInputs) }
                            NodeOutcome(nodeId, out, sub, null, start.elapsedNow().inWholeMilliseconds)
                        } catch (e: CancellationException) { throw e }
                        catch (e: Throwable) {
                            NodeOutcome(nodeId, null, null, e.message ?: e::class.simpleName ?: "Unknown", start.elapsedNow().inWholeMilliseconds)
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            for (o in outcomes) {
                durations[o.nodeId] = o.durationMs
                if (o.error == null && o.output != null) {
                    outputs[o.nodeId] = o.output
                    o.subResult?.let { subResults[o.nodeId] = it }
                    status[o.nodeId] = NodeExecutionStatus.Success
                    executed += o.nodeId
                    onEvent?.invoke(ExecutionEvent.Succeeded(o.nodeId, o.output, o.durationMs))
                } else {
                    val err = o.error ?: "Unknown error"
                    status[o.nodeId] = NodeExecutionStatus.Error
                    errors[o.nodeId] = err
                    executed += o.nodeId
                    onEvent?.invoke(ExecutionEvent.Failed(o.nodeId, err, o.durationMs))
                    dependents.transitiveDependentsOf(o.nodeId).forEach { dep ->
                        if (status[dep] == null) {
                            status[dep] = NodeExecutionStatus.Skipped
                            onEvent?.invoke(ExecutionEvent.Skipped(dep, causeNodeId = o.nodeId))
                        }
                    }
                }
            }
        }

        return WorkflowExecutionResult(
            nodeOutputsByNodeId = outputs,
            executionOrder = executed,
            statusByNodeId = status,
            errorsByNodeId = errors,
            durationsByNodeId = durations,
            subResults = subResults,
        )
    }

    private suspend fun runNode(
        workflow: WorkflowDefinition,
        node: NodeRef,
        outputs: Map<String, Map<String, WorkflowValue>>,
        externalInputs: Map<String, WorkflowValue>,
    ): Pair<Map<String, WorkflowValue>, WorkflowExecutionResult?> {
        val spec = nodeSpecs?.resolve(node.type)
        val inputs = buildInputMap(workflow, node, spec, outputs, externalInputs)

        val subgraph = node.subgraph
        if (subgraph != null) {
            // The subgraph node's resolved inputs flow down to fill the inner workflow's free ports.
            val inner = execute(subgraph, externalInputs = inputs)
            if (inner.errorCount > 0)
                throw WorkflowExecutionException("Subgraph '${node.id}' failed: ${inner.errorsByNodeId.values.firstOrNull()}")
            val innerOutputs = freeOutputs(subgraph, inner.nodeOutputsByNodeId)
            val executor = nodeExecutors.resolve(node.type)
            val out = if (executor != null) executor.execute(inputs + innerOutputs) else innerOutputs
            return out to inner
        }

        val executor = nodeExecutors.resolve(node.type)
            ?: throw WorkflowExecutionException("No executor registered for node type '${node.type}'.")
        return executor.execute(inputs) to null
    }
}
