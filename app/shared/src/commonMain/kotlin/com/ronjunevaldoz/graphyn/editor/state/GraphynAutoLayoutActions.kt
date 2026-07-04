package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics

internal data class AutoLayoutResult(
    val positions: Map<String, IntOffset>,
    val sizes: Map<String, IntSize>,
)

internal fun GraphynEditorState.performAutoLayout(): AutoLayoutResult? {
    val wf = workflow ?: return null
    val registry = canvasCards
    val nodeSize: (String) -> IntSize = { type -> resolveNodeSize(type) }
    val (annotationNodes, graphNodes) = wf.nodes.partition { registry?.resolve(it.type)?.isAnnotation == true }
    if (graphNodes.size > GraphynAutoLayout.MAX_NODES) {
        log.push("Auto-layout skipped: ${graphNodes.size} nodes exceeds limit of ${GraphynAutoLayout.MAX_NODES}")
        return null
    }
    val positions = GraphynAutoLayout.computePositions(graphNodes, wf.connections, nodeSize).toMutableMap()
    if (annotationNodes.isNotEmpty()) {
        val gap = GraphynCanvasMetrics.NodeSize.width
        val maxAnnW = annotationNodes.maxOf { nodeSize(it.type).width }
        val graphMinX = positions.values.minOfOrNull { it.x } ?: 0
        val graphMinY = positions.values.minOfOrNull { it.y } ?: 0
        // The annotation column sits to the left of the graph at graphMinX - maxAnnW - gap.
        // If that would go negative, shift the whole graph right first so the column still
        // fits at x=0 without overlapping — otherwise clamp() collapses it onto the graph.
        val annX = graphMinX - maxAnnW - gap
        if (annX < 0) {
            val shift = -annX
            positions.replaceAll { _, p -> IntOffset(p.x + shift, p.y) }
        }
        var annY = graphMinY
        annotationNodes.forEach { ann ->
            positions[ann.id] = IntOffset(maxOf(annX, 0), annY)
            annY += nodeSize(ann.type).height + GraphynCanvasMetrics.NodeSize.height
        }
    }
    positions.forEach { (id, pos) -> layout.setNodePosition(id, pos) }
    return AutoLayoutResult(positions, wf.nodes.associate { it.id to nodeSize(it.type) })
}
