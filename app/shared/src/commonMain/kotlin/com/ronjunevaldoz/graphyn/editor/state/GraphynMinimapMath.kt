package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
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

fun mapWorldPointToMinimap(
    worldPoint: Offset,
    layout: GraphynMinimapLayout,
): Offset = Offset(
    x = layout.insetX + ((worldPoint.x - layout.worldBounds.left) * layout.scale),
    y = layout.insetY + ((worldPoint.y - layout.worldBounds.top) * layout.scale),
)

fun mapWorldRectToMinimap(
    worldRect: Rect,
    layout: GraphynMinimapLayout,
): Rect {
    val topLeft = mapWorldPointToMinimap(
        worldPoint = Offset(worldRect.left, worldRect.top),
        layout = layout,
    )
    val bottomRight = mapWorldPointToMinimap(
        worldPoint = Offset(worldRect.right, worldRect.bottom),
        layout = layout,
    )
    return Rect(
        left = minOf(topLeft.x, bottomRight.x),
        top = minOf(topLeft.y, bottomRight.y),
        right = maxOf(topLeft.x, bottomRight.x),
        bottom = maxOf(topLeft.y, bottomRight.y),
    )
}

fun mapMinimapPointToWorld(
    minimapPoint: Offset,
    layout: GraphynMinimapLayout,
): Offset = Offset(
    x = layout.worldBounds.left + ((minimapPoint.x - layout.insetX) / layout.scale),
    y = layout.worldBounds.top + ((minimapPoint.y - layout.insetY) / layout.scale),
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
    return calculateMinimapLayout(
        worldBounds = worldBounds,
        minimapSize = minimapSize,
    )
}

fun calculateViewportRectInMinimap(
    viewport: GraphynViewport,
    canvasSize: IntSize,
    layout: GraphynMinimapLayout,
): Rect? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null

    val viewportTopLeft = viewport.screenToWorld(Offset.Zero)
    val viewportBottomRight = viewport.screenToWorld(
        Offset(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
    )
    val topLeft = mapWorldPointToMinimap(viewportTopLeft, layout)
    val bottomRight = mapWorldPointToMinimap(viewportBottomRight, layout)

    val left = minOf(topLeft.x, bottomRight.x)
    val top = minOf(topLeft.y, bottomRight.y)
    val right = maxOf(topLeft.x, bottomRight.x)
    val bottom = maxOf(topLeft.y, bottomRight.y)

    return Rect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )
}

fun viewportCenteredOnWorldPoint(
    currentViewport: GraphynViewport,
    canvasSize: IntSize,
    worldPoint: Offset,
): GraphynViewport {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return currentViewport
    val screenCenter = Offset(
        x = canvasSize.width / 2f,
        y = canvasSize.height / 2f,
    )
    return currentViewport.copy(
        offset = screenCenter - (worldPoint * currentViewport.scale),
    )
}

fun calculateWorldBounds(
    nodePositions: List<IntOffset>,
    nodeSize: IntSize,
    padding: Float = 0f,
): Rect {
    if (nodePositions.isEmpty()) {
        return Rect(0f, 0f, 1f, 1f)
    }

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
