package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset

internal const val GraphynCanvasInset = 240

data class GraphynCanvasBounds(
    val width: Int = DefaultLogicalCanvasWidth,
    val height: Int = DefaultLogicalCanvasHeight,
) {
    fun toRect(): Rect = Rect(
        left = 0f,
        top = 0f,
        right = width.toFloat(),
        bottom = height.toFloat(),
    )

    companion object {
        const val DefaultLogicalCanvasWidth = 8192
        const val DefaultLogicalCanvasHeight = 6144
    }
}

object GraphynCanvasLayout {
    private const val DEFAULT_COLUMNS = 3
    private const val CELL_WIDTH = 280
    private const val CELL_HEIGHT = 180
    private const val HORIZONTAL_GAP = 24
    private const val VERTICAL_GAP = 24

    fun fallbackPosition(index: Int, bounds: GraphynCanvasBounds = GraphynCanvasBounds()): IntOffset {
        val column = index % DEFAULT_COLUMNS
        val row = index / DEFAULT_COLUMNS
        val insetX = GraphynCanvasInset.coerceAtMost((bounds.width / 4).coerceAtLeast(0))
        val insetY = GraphynCanvasInset.coerceAtMost((bounds.height / 4).coerceAtLeast(0))
        val maxX = (bounds.width - CELL_WIDTH - insetX).coerceAtLeast(insetX)
        val maxY = (bounds.height - CELL_HEIGHT - insetY).coerceAtLeast(insetY)
        val x = (insetX + column * (CELL_WIDTH + HORIZONTAL_GAP)).coerceAtMost(maxX)
        val y = (insetY + row * (CELL_HEIGHT + VERTICAL_GAP)).coerceAtMost(maxY)
        return IntOffset(x, y)
    }

    fun logicalCanvasBounds(bounds: GraphynCanvasBounds = GraphynCanvasBounds()): Rect = bounds.toRect()
}
