package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

/** Resolves a node type's real rendered size from the canvas card registry, falling back to the field-card shape. */
internal fun GraphynEditorState.resolveNodeSize(type: String): IntSize =
    canvasCards?.resolve(type)?.let { IntSize(it.nodeWidth, it.nodeHeight) }
        ?: nodeSpecs?.resolve(type)?.let { spec ->
            val factory = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
            IntSize(factory.nodeWidth, factory.nodeHeight)
        }
        ?: GraphynCanvasMetrics.NodeSize

/** Same as [resolveNodeSize] but looked up by node id, for callers that only have the id (drag, clamp, hit-testing). */
internal fun GraphynEditorState.resolveNodeSizeById(nodeId: String): IntSize {
    val type = workflow?.nodes?.firstOrNull { it.id == nodeId }?.type ?: return GraphynCanvasMetrics.NodeSize
    return resolveNodeSize(type)
}
