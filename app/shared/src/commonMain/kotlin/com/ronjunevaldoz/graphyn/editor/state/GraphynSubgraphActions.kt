package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.GRAPHYN_SUBGRAPH_TYPE
import com.ronjunevaldoz.graphyn.core.model.collapseToSubgraph
import com.ronjunevaldoz.graphyn.core.model.expandSubgraph
import kotlin.random.Random

/**
 * Collapses the current selection into a single [GRAPHYN_SUBGRAPH_TYPE] node, positioned at the
 * centroid of the collapsed nodes. No-op for fewer than two selected nodes. The new node's ports
 * are derived from the inner workflow's boundary (see `deriveSubgraphSpec`), so it renders and
 * wires correctly without a registered spec.
 */
internal fun GraphynEditorState.collapseSelectionToSubgraph() {
    val wf = workflow ?: return
    val ids = effectiveSelectedNodeIds
    if (ids.size < 2) return

    val newId = "subgraph-${Random.nextLong().and(0xFFFFFFFFL)}"
    val result = collapseToSubgraph(wf, ids, newId, GRAPHYN_SUBGRAPH_TYPE) ?: return

    val centroid = centroidOf(ids)
    workflow = result.workflow
    ids.forEach { layout.removeNode(it) }
    layout.setNodePosition(newId, centroid)
    selectedNodeIds = emptySet()
    selectedNodeId = newId
    log.push("Collapsed ${ids.size} nodes into subgraph $newId")
}

/**
 * Expands subgraph [nodeId] back into its inner nodes inline, reattaching external connections
 * to the inner nodes that own the matching boundary ports. Inner nodes are laid out in a row
 * starting at the subgraph node's former position.
 */
internal fun GraphynEditorState.expandSubgraphNode(nodeId: String) {
    val wf = workflow ?: return
    val specs = nodeSpecs ?: return
    val node = wf.nodes.firstOrNull { it.id == nodeId } ?: return
    val inner = node.subgraph ?: return
    val expanded = expandSubgraph(wf, nodeId, specs) ?: return

    val origin = layout.nodePositionsByNodeId[nodeId] ?: IntOffset.Zero
    workflow = expanded
    layout.removeNode(nodeId)
    inner.nodes.forEachIndexed { i, n ->
        layout.setNodePosition(n.id, IntOffset(origin.x + i * STEP_X, origin.y))
    }
    selectedNodeId = null
    selectedNodeIds = inner.nodes.mapTo(mutableSetOf()) { it.id }
    log.push("Expanded subgraph $nodeId into ${inner.nodes.size} nodes")
}

private const val STEP_X = 300

private fun GraphynEditorState.centroidOf(ids: Set<String>): IntOffset {
    val positions = ids.mapNotNull { layout.nodePositionsByNodeId[it] }
    if (positions.isEmpty()) return IntOffset.Zero
    return IntOffset(positions.sumOf { it.x } / positions.size, positions.sumOf { it.y } / positions.size)
}
