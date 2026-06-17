package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

data class GraphynMinimapLayout(
    val worldBounds: Rect,
    val scale: Float,
    val insetX: Float,
    val insetY: Float,
)

fun Rect.union(other: Rect): Rect = Rect(
    left = minOf(left, other.left),
    top = minOf(top, other.top),
    right = maxOf(right, other.right),
    bottom = maxOf(bottom, other.bottom),
)

fun calculateMinimapLayout(
    worldBounds: Rect,
    minimapSize: IntSize,
): GraphynMinimapLayout {
    val contentWidth = worldBounds.width.coerceAtLeast(1f)
    val contentHeight = worldBounds.height.coerceAtLeast(1f)
    val scaleX = minimapSize.width / contentWidth
    val scaleY = minimapSize.height / contentHeight
    val scale = minOf(scaleX, scaleY)
    val insetX = (minimapSize.width - (contentWidth * scale)) / 2f
    val insetY = (minimapSize.height - (contentHeight * scale)) / 2f
    return GraphynMinimapLayout(
        worldBounds = worldBounds,
        scale = scale,
        insetX = insetX,
        insetY = insetY,
    )
}

fun calculateMinimapLayout(
    nodePositions: List<IntOffset>,
    nodeSize: IntSize,
    minimapSize: IntSize,
    padding: Float = 0f,
): GraphynMinimapLayout {
    val worldBounds = calculateWorldBounds(nodePositions, nodeSize, padding)
    return calculateMinimapLayout(worldBounds = worldBounds, minimapSize = minimapSize)
}

fun calculateWorldBounds(
    nodePositions: List<IntOffset>,
    nodeSize: IntSize,
    padding: Float = 0f,
): Rect {
    if (nodePositions.isEmpty()) return Rect(0f, 0f, 1f, 1f)

    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY

    nodePositions.forEach { position ->
        left = minOf(left, position.x.toFloat())
        top = minOf(top, position.y.toFloat())
        right = maxOf(right, position.x.toFloat() + nodeSize.width)
        bottom = maxOf(bottom, position.y.toFloat() + nodeSize.height)
    }

    if (!left.isFinite() || !top.isFinite() || !right.isFinite() || !bottom.isFinite()) {
        return Rect(0f, 0f, 1f, 1f)
    }

    val minWidth = nodeSize.width + (padding * 2f)
    val minHeight = nodeSize.height + (padding * 2f)
    return Rect(
        left - padding,
        top - padding,
        maxOf(right + padding, left - padding + minWidth),
        maxOf(bottom + padding, top - padding + minHeight),
    )
}
