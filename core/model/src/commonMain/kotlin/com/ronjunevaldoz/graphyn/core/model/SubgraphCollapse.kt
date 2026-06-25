package com.ronjunevaldoz.graphyn.core.model

import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

/**
 * Node type for editor-created subgraphs. Has no statically-registered [NodeSpec] — its ports are
 * derived from the embedded workflow's boundary via [deriveSubgraphSpec].
 */
const val GRAPHYN_SUBGRAPH_TYPE = "graphyn.subgraph"

/** Result of collapsing nodes into a subgraph: the rewritten workflow plus the new node's id. */
data class CollapseResult(
    val workflow: WorkflowDefinition,
    val subgraphNodeId: String,
)

/**
 * Collapses [selectedIds] of [workflow] into a single subgraph node of type [subgraphType].
 *
 * The selected nodes and the connections *between* them move into the new node's embedded
 * [WorkflowDefinition]. Connections crossing the selection boundary are rewired to the
 * subgraph node, matched by the inner port's name (so they line up with the boundary the
 * execution engine injects into / reads from). Returns null when fewer than two nodes are
 * selected or any id is unknown.
 *
 * Boundary ports are keyed by inner port name; if two crossing edges share a port name they
 * collapse onto one subgraph port (documented limitation, mirrors the engine's by-name model).
 */
fun collapseToSubgraph(
    workflow: WorkflowDefinition,
    selectedIds: Set<String>,
    newNodeId: String,
    subgraphType: String,
    innerName: String = "Subgraph",
): CollapseResult? {
    if (selectedIds.size < 2) return null
    val inner = workflow.nodes.filter { it.id in selectedIds }
    if (inner.size != selectedIds.size) return null

    val internal = workflow.connections.filter { it.fromNodeId in selectedIds && it.toNodeId in selectedIds }
    val innerDef = WorkflowDefinition(
        id = newNodeId, name = innerName, nodes = inner, connections = internal,
    )

    val rewired = workflow.connections.mapNotNull { c ->
        val fromIn = c.fromNodeId in selectedIds
        val toIn = c.toNodeId in selectedIds
        when {
            fromIn && toIn -> null // absorbed into the subgraph
            fromIn -> c.copy(fromNodeId = newNodeId, fromPort = c.fromPort) // boundary-out
            toIn -> c.copy(toNodeId = newNodeId, toPort = c.toPort)         // boundary-in
            else -> c
        }
    }.distinct()

    val remainingNodes = workflow.nodes.filterNot { it.id in selectedIds }
    val subgraphNode = NodeRef(id = newNodeId, type = subgraphType, subgraph = innerDef)
    return CollapseResult(
        workflow = workflow.copy(nodes = remainingNodes + subgraphNode, connections = rewired),
        subgraphNodeId = newNodeId,
    )
}

/**
 * Inverse of [collapseToSubgraph]: replaces the subgraph [nodeId] with its inner nodes inline.
 *
 * Inner connections are restored verbatim. External edges that referenced the subgraph node are
 * reattached to the inner node owning the matching boundary port (resolved via [specs]). Edges
 * whose port no longer maps to any inner node are dropped. Returns null when [nodeId] is not a
 * subgraph node.
 */
fun expandSubgraph(
    workflow: WorkflowDefinition,
    nodeId: String,
    specs: NodeSpecRegistry,
): WorkflowDefinition? {
    val node = workflow.nodes.firstOrNull { it.id == nodeId } ?: return null
    val inner = node.subgraph ?: return null

    val inputOwner = portOwner(inner, specs, isInput = true)
    val outputOwner = portOwner(inner, specs, isInput = false)

    val external = workflow.connections.filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId }
    val reattached = workflow.connections.mapNotNull { c ->
        when (nodeId) {
            c.toNodeId -> inputOwner[c.toPort]?.let { c.copy(toNodeId = it) }
            c.fromNodeId -> outputOwner[c.fromPort]?.let { c.copy(fromNodeId = it) }
            else -> null
        }
    }

    val remaining = workflow.nodes.filterNot { it.id == nodeId }
    return workflow.copy(
        nodes = remaining + inner.nodes,
        connections = external + inner.connections + reattached,
    )
}

private fun portOwner(inner: WorkflowDefinition, specs: NodeSpecRegistry, isInput: Boolean): Map<String, String> {
    val filled = inner.connections.mapTo(mutableSetOf()) {
        if (isInput) it.toNodeId to it.toPort else it.fromNodeId to it.fromPort
    }
    val owner = linkedMapOf<String, String>()
    inner.nodes.forEach { n ->
        val ports = specs.resolve(n.type)?.let { if (isInput) it.inputs else it.outputs }.orEmpty()
        ports.forEach { p ->
            if ((n.id to p.name) !in filled && p.name !in owner) owner[p.name] = n.id
        }
    }
    return owner
}
