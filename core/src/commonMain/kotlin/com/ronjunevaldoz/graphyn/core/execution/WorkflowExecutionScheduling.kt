package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Groups nodes into layers where every node in layer N depends only on nodes in layers 0..N-1.
 * Nodes within the same layer have no data dependency on each other and can run in parallel.
 */
internal fun WorkflowDefinition.topologicalLayers(): List<List<String>> {
    val incoming = nodes.associate { it.id to 0 }.toMutableMap()
    val adjacency = connections.groupBy(ConnectionRef::fromNodeId)
        .mapValues { (_, refs) -> refs.map(ConnectionRef::toNodeId) }
    connections.forEach { incoming[it.toNodeId] = (incoming[it.toNodeId] ?: 0) + 1 }

    val layers = mutableListOf<List<String>>()
    var ready = nodes.filter { incoming[it.id] == 0 }.map { it.id }
    while (ready.isNotEmpty()) {
        layers += ready
        val next = mutableListOf<String>()
        ready.forEach { nodeId ->
            adjacency[nodeId].orEmpty().forEach { s ->
                incoming[s] = (incoming[s] ?: 0) - 1
                if (incoming[s] == 0) next += s
            }
        }
        ready = next
    }
    if (layers.sumOf { it.size } != nodes.size)
        throw WorkflowExecutionException("Workflow contains a cycle and cannot be executed.")
    return layers
}

/** Kahn topological sort. Throws [WorkflowExecutionException] if the graph contains a cycle. */
internal fun WorkflowDefinition.topologicalOrder(): List<String> {
    val incoming = nodes.associate { it.id to 0 }.toMutableMap()
    val adjacency = connections.groupBy(ConnectionRef::fromNodeId)
        .mapValues { (_, refs) -> refs.map(ConnectionRef::toNodeId) }
    connections.forEach { incoming[it.toNodeId] = (incoming[it.toNodeId] ?: 0) + 1 }

    val queue = ArrayDeque(nodes.filter { incoming[it.id] == 0 }.map { it.id })
    val order = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!seen.add(current)) continue
        order += current
        adjacency[current].orEmpty().forEach { next ->
            val remaining = (incoming[next] ?: 0) - 1
            incoming[next] = remaining
            if (remaining == 0) queue.add(next)
        }
    }
    if (order.size != nodes.size) {
        throw WorkflowExecutionException("Workflow contains a cycle and cannot be executed.")
    }
    return order
}

/** Direct successor node ids keyed by source node id. */
internal fun WorkflowDefinition.directDependents(): Map<String, List<String>> =
    connections.groupBy(ConnectionRef::fromNodeId)
        .mapValues { (_, refs) -> refs.map(ConnectionRef::toNodeId).distinct() }

/** All nodes reachable downstream of [nodeId] (its transitive dependents), excluding [nodeId] itself. */
internal fun Map<String, List<String>>.transitiveDependentsOf(nodeId: String): Set<String> {
    val result = mutableSetOf<String>()
    val stack = ArrayDeque(this[nodeId].orEmpty())
    while (stack.isNotEmpty()) {
        val n = stack.removeLast()
        if (result.add(n)) stack.addAll(this[n].orEmpty())
    }
    return result
}

/**
 * Resolves the input map for [node], applying sources in increasing priority:
 * spec defaults < [externalInputs] < node config < connected upstream outputs.
 *
 * [externalInputs] are values handed down from an enclosing subgraph node, keyed by port name.
 * They fill a node's free input ports (those not set by config or an internal connection) so a
 * nested workflow can receive data from its parent. Explicit config and internal wiring always win.
 *
 * Only connections whose source produced output are wired (failed/skipped sources contribute nothing).
 */
internal fun buildInputMap(
    workflow: WorkflowDefinition,
    node: NodeRef,
    spec: NodeSpec?,
    outputsByNodeId: Map<String, Map<String, WorkflowValue>>,
    externalInputs: Map<String, WorkflowValue> = emptyMap(),
): Map<String, WorkflowValue> {
    val connected = linkedMapOf<String, WorkflowValue>()
    workflow.connections.filter { it.toNodeId == node.id }.forEach { conn ->
        val value = outputsByNodeId[conn.fromNodeId]?.get(conn.fromPort) ?: return@forEach
        connected[conn.toPort] = value
    }
    return (spec?.defaultValues.orEmpty()) + externalInputs + node.config + connected
}
