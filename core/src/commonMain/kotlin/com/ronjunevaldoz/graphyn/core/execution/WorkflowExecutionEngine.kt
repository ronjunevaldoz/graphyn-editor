package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowExecutionResult(
    val nodeOutputsByNodeId: Map<String, Map<String, WorkflowValue>>,
    val executionOrder: List<String>,
    /** Nested results for any nodes whose [NodeRef.subgraph] was executed. */
    val subResults: Map<String, WorkflowExecutionResult> = emptyMap(),
)

class WorkflowExecutionException(
    message: String,
) : IllegalStateException(message)

class WorkflowExecutionEngine(
    private val nodeExecutors: NodeExecutorRegistry,
    private val nodeSpecs: NodeSpecRegistry? = null,
) {
    suspend fun execute(workflow: WorkflowDefinition): WorkflowExecutionResult {
        val nodesById = workflow.nodes.associateBy { it.id }
        if (nodesById.size != workflow.nodes.size) {
            throw WorkflowExecutionException("Workflow contains duplicate node ids.")
        }

        val order = topologicalOrder(workflow)
        val outputsByNodeId = linkedMapOf<String, Map<String, WorkflowValue>>()
        val subResults = linkedMapOf<String, WorkflowExecutionResult>()

        for (nodeId in order) {
            val node = nodesById.getValue(nodeId)
            val spec = nodeSpecs?.resolve(node.type)

            // Nodes with an embedded subgraph execute their inner workflow recursively.
            // The sink node's outputs become this node's outputs; no registered executor needed.
            if (node.subgraph != null) {
                val inner = execute(node.subgraph!!)
                val sinkId = inner.executionOrder.lastOrNull()
                outputsByNodeId[node.id] = sinkId?.let { inner.nodeOutputsByNodeId[it] } ?: emptyMap()
                subResults[node.id] = inner
                continue
            }

            val executor = nodeExecutors.resolve(node.type)
                ?: throw WorkflowExecutionException("No executor registered for node type '${node.type}'.")
            val inputs = buildInputMap(workflow, node, spec, outputsByNodeId)
            outputsByNodeId[node.id] = executor.execute(inputs)
        }

        return WorkflowExecutionResult(
            nodeOutputsByNodeId = outputsByNodeId,
            executionOrder = order,
            subResults = subResults,
        )
    }

    private fun buildInputMap(
        workflow: WorkflowDefinition,
        node: NodeRef,
        spec: NodeSpec?,
        outputsByNodeId: Map<String, Map<String, WorkflowValue>>,
    ): Map<String, WorkflowValue> {
        val connectedInputs = workflow.connections
            .filter { it.toNodeId == node.id }
            .associateNotNull { connection ->
                val sourceOutputs = outputsByNodeId[connection.fromNodeId] ?: return@associateNotNull null
                sourceOutputs[connection.fromPort]?.let { value ->
                    connection.toPort to value
                }
            }

        val defaults = spec?.defaultValues.orEmpty()
        return defaults + node.config + connectedInputs
    }

    private fun topologicalOrder(workflow: WorkflowDefinition): List<String> {
        val incomingCount = workflow.nodes.associate { node ->
            node.id to 0
        }.toMutableMap()
        val adjacency = workflow.connections.groupBy(ConnectionRef::fromNodeId)
            .mapValues { (_, refs) -> refs.map(ConnectionRef::toNodeId) }

        workflow.connections.forEach { connection ->
            incomingCount[connection.toNodeId] = (incomingCount[connection.toNodeId] ?: 0) + 1
        }

        val queue = ArrayDeque<String>()
        workflow.nodes.filter { incomingCount[it.id] == 0 }.forEach { queue.add(it.id) }

        val order = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!seen.add(current)) continue
            order += current

            adjacency[current].orEmpty().forEach { next ->
                val remaining = (incomingCount[next] ?: 0) - 1
                incomingCount[next] = remaining
                if (remaining == 0) {
                    queue.add(next)
                }
            }
        }

        if (order.size != workflow.nodes.size) {
            throw WorkflowExecutionException("Workflow contains a cycle and cannot be executed.")
        }

        return order
    }
}

private inline fun <K, V, R> Iterable<V>.associateNotNull(transform: (V) -> Pair<K, R>?): Map<K, R> {
    val destination = linkedMapOf<K, R>()
    for (element in this) {
        val pair = transform(element) ?: continue
        destination[pair.first] = pair.second
    }
    return destination
}
