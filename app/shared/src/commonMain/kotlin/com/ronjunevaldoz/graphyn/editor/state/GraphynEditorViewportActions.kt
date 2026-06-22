package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics

/** Records a canvas resize and re-fits if the viewport is still pinned to a fit (see [fitToContent]). */
fun GraphynEditorState.updateCanvasSize(size: IntSize) {
    val changed = size != viewportState.canvasSize
    if (changed) telemetry.push("canvasSize -> ${size.width}x${size.height}")
    viewportState.updateCanvasSize(size)
    if (changed && autoFitOnResize && size.width > 0 && size.height > 0) fitToContent()
}

/** Manual pan/zoom: stop auto-refitting so we don't override the user's navigation. */
fun GraphynEditorState.updateViewportTransform(pan: Offset, zoom: Float, focus: Offset) {
    autoFitOnResize = false
    viewportState.updateTransform(pan, zoom, focus)
}

fun GraphynEditorState.resetViewport() {
    viewportState.reset()
    log.push("Viewport reset")
}

/**
 * Fits the viewport to [positions] (defaulting to current node positions), centered and contained.
 * Pins the viewport so subsequent canvas resizes re-fit, and emits fit telemetry to the DEBUG tab.
 */
fun GraphynEditorState.fitToContent(
    positions: Map<String, IntOffset>? = null,
    sizes: Map<String, IntSize> = emptyMap(),
) {
    val resolvedPositions = positions ?: layout.nodePositionsByNodeId
    val resolvedSizes = sizes.ifEmpty {
        workflow?.nodes?.associate { node ->
            node.id to (canvasCards?.resolve(node.type)
                ?.let { IntSize(it.nodeWidth, it.nodeHeight) }
                ?: GraphynCanvasMetrics.NodeSize)
        } ?: emptyMap()
    }
    viewportState.fitToPositions(resolvedPositions, resolvedSizes, maxScale = 1.0f)
    autoFitOnResize = resolvedPositions.isNotEmpty()
    if (resolvedPositions.isEmpty()) return

    val vp = viewportState.viewport
    val cs = viewportState.canvasSize
    val default = GraphynCanvasMetrics.NodeSize
    val minX = resolvedPositions.values.minOf { it.x.toFloat() }
    val maxX = resolvedPositions.entries.maxOf { (id, p) -> p.x + (resolvedSizes[id]?.width ?: default.width).toFloat() }
    val minY = resolvedPositions.values.minOf { it.y.toFloat() }
    val maxY = resolvedPositions.entries.maxOf { (id, p) -> p.y + (resolvedSizes[id]?.height ?: default.height).toFloat() }
    val lGap = (minX * vp.scale + vp.offset.x).toInt()
    val rGap = (cs.width - (maxX * vp.scale + vp.offset.x)).toInt()
    val tGap = (minY * vp.scale + vp.offset.y).toInt()
    val bGap = (cs.height - (maxY * vp.scale + vp.offset.y)).toInt()
    val s = (vp.scale * 1000).toInt() / 1000f
    telemetry.push("fit: canvas=${cs.width}x${cs.height} scale=$s off=(${vp.offset.x.toInt()},${vp.offset.y.toInt()}) L=$lGap R=$rGap T=$tGap B=$bGap")
}
