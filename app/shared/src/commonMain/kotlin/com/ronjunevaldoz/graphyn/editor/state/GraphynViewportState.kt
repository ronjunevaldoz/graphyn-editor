package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout

internal class GraphynViewportState(
    private val canvasBounds: GraphynCanvasBounds,
) {
    companion object {
        const val MinScale = 0.45f
        const val MaxScale = 5.0f
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

    fun screenToWorld(position: Offset): Offset = viewport.screenToWorld(position)
    fun worldToScreen(position: Offset): Offset = viewport.worldToScreen(position)
}
