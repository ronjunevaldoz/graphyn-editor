package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics

internal class GraphynViewportState(
    private val canvasBounds: GraphynCanvasBounds,
) {
    companion object {
        const val MinScale = 0.45f
        const val MaxScale = 5.0f
        // Fit-to-content may zoom out below the interactive MinScale so a wide graph
        // can be fully contained in a narrow canvas instead of spilling past the edges.
        const val MinFitScale = 0.05f
    }

    var viewport: GraphynViewport by mutableStateOf(GraphynViewport())
    var canvasSize by mutableStateOf(IntSize.Zero)
    var graphWorldBounds by mutableStateOf<Rect?>(null)
    private var trackedNodeIds: Set<String> = emptySet()

    fun refresh(nodeIds: Set<String>) {
        if (nodeIds == trackedNodeIds) return
        trackedNodeIds = nodeIds
        graphWorldBounds = if (nodeIds.isEmpty()) null else GraphynCanvasLayout.logicalCanvasBounds(canvasBounds)
        viewport = viewport.constrainTo(graphWorldBounds, canvasSize)
    }

    fun updateTransform(pan: Offset, zoom: Float, focus: Offset) {
        viewport = viewport.zoomAt(focus, zoom, MinScale, MaxScale)
        if (pan != Offset.Zero) viewport = viewport.panBy(pan)
        viewport = viewport.constrainTo(graphWorldBounds, canvasSize)
    }

    fun reset() {
        viewport = GraphynViewport()
        viewport = viewport.constrainTo(graphWorldBounds, canvasSize)
    }

    fun updateCanvasSize(size: IntSize) {
        canvasSize = size
    }

    fun fitToPositions(
        positions: Map<String, IntOffset>,
        sizes: Map<String, IntSize> = emptyMap(),
        maxScale: Float = MaxScale,
    ) {
        if (positions.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return
        val padding = 60f
        val default = GraphynCanvasMetrics.NodeSize
        val minX = positions.values.minOf { it.x.toFloat() }
        val minY = positions.values.minOf { it.y.toFloat() }
        val maxX = positions.entries.maxOf { (id, p) -> p.x + (sizes[id]?.width ?: default.width).toFloat() }
        val maxY = positions.entries.maxOf { (id, p) -> p.y + (sizes[id]?.height ?: default.height).toFloat() }
        val scale = minOf(
            (canvasSize.width - padding * 2) / (maxX - minX),
            (canvasSize.height - padding * 2) / (maxY - minY),
            maxScale,
        ).coerceAtLeast(MinFitScale)
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        viewport = GraphynViewport(
            offset = Offset(canvasSize.width / 2f - cx * scale, canvasSize.height / 2f - cy * scale),
            scale = scale,
        ).constrainTo(graphWorldBounds, canvasSize)
    }

    fun screenToWorld(position: Offset): Offset = viewport.screenToWorld(position)
    fun worldToScreen(position: Offset): Offset = viewport.worldToScreen(position)
}
