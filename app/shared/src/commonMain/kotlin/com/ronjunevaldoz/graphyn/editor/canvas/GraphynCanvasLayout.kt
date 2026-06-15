package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset

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
        const val DefaultLogicalCanvasWidth = 4096
        const val DefaultLogicalCanvasHeight = 3072
    }
}

object GraphynCanvasLayout {
    private const val DEFAULT_COLUMNS = 3
    private const val CELL_WIDTH = 280
    private const val CELL_HEIGHT = 180
    private const val HORIZONTAL_GAP = 24
    private const val VERTICAL_GAP = 24

    fun fallbackPosition(index: Int): IntOffset {
        val column = index % DEFAULT_COLUMNS
        val row = index / DEFAULT_COLUMNS
        val x = column * (CELL_WIDTH + HORIZONTAL_GAP)
        val y = row * (CELL_HEIGHT + VERTICAL_GAP)
        return IntOffset(x, y)
    }

    fun logicalCanvasBounds(bounds: GraphynCanvasBounds = GraphynCanvasBounds()): Rect = bounds.toRect()
}
