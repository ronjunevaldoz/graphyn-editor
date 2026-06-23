package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.TimeSource

class WorkflowExecutionException(message: String) : IllegalStateException(message)

/**
 * Runs a [WorkflowDefinition] in topological order.
 *
 * Execution is **resilient**: a node that throws is recorded as an error and its transitive
 * dependents are skipped, but independent branches keep running. The returned
 * [WorkflowExecutionResult] therefore reports per-node status and may be partial.
 *
 * Pass [onEvent] to observe progress live (e.g. highlight nodes on a canvas as they run).
 */
class WorkflowExecutionEngine(
    private val nodeExecutors: NodeExecutorRegistry,
    private val nodeSpecs: NodeSpecRegistry? = null,
) {
    suspend fun execute(
        workflow: WorkflowDefinition,
        onEvent: ((ExecutionEvent) -> Unit)? = null,
    ): WorkflowExecutionResult {
        val nodesById = workflow.nodes.associateBy { it.id }
        if (nodesById.size != workflow.nodes.size) {
            throw WorkflowExecutionException("Workflow contains duplicate node ids.")
        }

        val order = workflow.topologicalOrder()
        val dependents = workflow.directDependents()

        val outputs = linkedMapOf<String, Map<String, WorkflowValue>>()
        val status = linkedMapOf<String, NodeExecutionStatus>()
        val errors = linkedMapOf<String, String>()
        val durations = linkedMapOf<String, Long>()
        val subResults = linkedMapOf<String, WorkflowExecutionResult>()
        val executed = mutableListOf<String>()

        for (nodeId in order) {
            if (status[nodeId] == NodeExecutionStatus.Skipped) continue

            val node = nodesById.getValue(nodeId)
            onEvent?.invoke(ExecutionEvent.Started(nodeId))
            val start = TimeSource.Monotonic.markNow()
            try {
                outputs[nodeId] = runWithPolicy(node) { runNode(workflow, node, outputs, subResults) }
                val ms = start.elapsedNow().inWholeMilliseconds
                durations[nodeId] = ms
                status[nodeId] = NodeExecutionStatus.Success
                executed += nodeId
                onEvent?.invoke(ExecutionEvent.Succeeded(nodeId, outputs.getValue(nodeId), ms))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val ms = start.elapsedNow().inWholeMilliseconds
                durations[nodeId] = ms
                status[nodeId] = NodeExecutionStatus.Error
                errors[nodeId] = e.message ?: e::class.simpleName ?: "Unknown error"
                executed += nodeId
                onEvent?.invoke(ExecutionEvent.Failed(nodeId, errors.getValue(nodeId), ms))
                dependents.transitiveDependentsOf(nodeId).forEach { dep ->
                    if (status[dep] == null) {
                        status[dep] = NodeExecutionStatus.Skipped
                        onEvent?.invoke(ExecutionEvent.Skipped(dep, causeNodeId = nodeId))
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

    /** Executes one node — either its embedded subgraph or its registered executor. */
    private suspend fun runNode(
        workflow: WorkflowDefinition,
        node: NodeRef,
        outputs: Map<String, Map<String, WorkflowValue>>,
        subResults: MutableMap<String, WorkflowExecutionResult>,
    ): Map<String, WorkflowValue> {
        val spec = nodeSpecs?.resolve(node.type)
        val inputs = buildInputMap(workflow, node, spec, outputs)

        val subgraph = node.subgraph
        if (subgraph != null) {
            val inner = execute(subgraph)
            subResults[node.id] = inner
            if (inner.errorCount > 0) {
                throw WorkflowExecutionException("Subgraph '${node.id}' failed: ${inner.errorsByNodeId.values.firstOrNull()}")
            }
            val sinkId = inner.executionOrder.lastOrNull()
            val innerOutputs = sinkId?.let { inner.nodeOutputsByNodeId[it] } ?: emptyMap()
            val executor = nodeExecutors.resolve(node.type)
            return if (executor != null) executor.execute(inputs + innerOutputs) else innerOutputs
        }

        val executor = nodeExecutors.resolve(node.type)
            ?: throw WorkflowExecutionException("No executor registered for node type '${node.type}'.")
        return executor.execute(inputs)
    }

    private suspend fun runWithPolicy(
        node: NodeRef,
        block: suspend () -> Map<String, WorkflowValue>,
    ): Map<String, WorkflowValue> {
        val attempts = (node.maxRetries.coerceAtLeast(0)) + 1
        var lastError: Throwable = WorkflowExecutionException("No attempts made")
        repeat(attempts) {
            try {
                return if (node.timeoutMs != null) {
                    try {
                        withTimeout(node.timeoutMs) { block() }
                    } catch (e: TimeoutCancellationException) {
                        throw WorkflowExecutionException("Node '${node.id}' timed out after ${node.timeoutMs}ms")
                    }
                } else {
                    block()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) { lastError = e }
        }
        throw lastError
    }
}
