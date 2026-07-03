package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

internal data class AutoLayoutResult(
    val positions: Map<String, IntOffset>,
    val sizes: Map<String, IntSize>,
)

internal fun GraphynEditorState.performAutoLayout(): AutoLayoutResult? {
    val wf = workflow ?: return null
    val registry = canvasCards
    val nodeSize: (String) -> IntSize = { type ->
        registry?.resolve(type)?.let { IntSize(it.nodeWidth, it.nodeHeight) }
            ?: nodeSpecs?.resolve(type)?.let { spec ->
                val factory = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
                IntSize(factory.nodeWidth, factory.nodeHeight)
            }
            ?: GraphynCanvasMetrics.NodeSize
    }
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
        var annY = graphMinY
        annotationNodes.forEach { ann ->
            positions[ann.id] = IntOffset(graphMinX - maxAnnW - gap, annY)
            annY += nodeSize(ann.type).height + GraphynCanvasMetrics.NodeSize.height
        }
    }
    positions.forEach { (id, pos) -> layout.setNodePosition(id, pos) }
    return AutoLayoutResult(positions, wf.nodes.associate { it.id to nodeSize(it.type) })
}
