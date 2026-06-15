package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

data class GraphynViewport(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
) {
    fun screenToWorld(position: Offset): Offset {
        if (scale == 0f) return position
        return (position - offset) / scale
    }

    fun worldToScreen(position: Offset): Offset = (position * scale) + offset

    fun panBy(delta: Offset): GraphynViewport = copy(offset = offset + delta)

    fun zoomAt(
        focus: Offset,
        factor: Float,
        minScale: Float,
        maxScale: Float,
    ): GraphynViewport {
        if (factor == 1f) return this
        val nextScale = (scale * factor).coerceIn(minScale, maxScale)
        if (nextScale == scale) return this
        val worldFocus = screenToWorld(focus)
        return copy(
            scale = nextScale,
            offset = focus - (worldFocus * nextScale),
        )
    }
}

fun GraphynViewport.constrainTo(
    worldBounds: Rect?,
    canvasSize: IntSize,
): GraphynViewport {
    if (worldBounds == null || canvasSize.width <= 0 || canvasSize.height <= 0) {
        return this
    }

    val minOffsetX = canvasSize.width - (worldBounds.right * scale)
    val maxOffsetX = -worldBounds.left * scale
    val minOffsetY = canvasSize.height - (worldBounds.bottom * scale)
    val maxOffsetY = -worldBounds.top * scale

    fun clamp(value: Float, min: Float, max: Float): Float {
        return if (min <= max) {
            value.coerceIn(min, max)
        } else {
            (min + max) / 2f
        }
    }

    return copy(
        offset = Offset(
            x = clamp(offset.x, minOffsetX, maxOffsetX),
            y = clamp(offset.y, minOffsetY, maxOffsetY),
        ),
    )
}
