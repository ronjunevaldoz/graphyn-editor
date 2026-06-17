package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

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
    val topLeft = mapWorldPointToMinimap(Offset(worldRect.left, worldRect.top), layout)
    val bottomRight = mapWorldPointToMinimap(Offset(worldRect.right, worldRect.bottom), layout)
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
    return Rect(
        left = minOf(topLeft.x, bottomRight.x),
        top = minOf(topLeft.y, bottomRight.y),
        right = maxOf(topLeft.x, bottomRight.x),
        bottom = maxOf(topLeft.y, bottomRight.y),
    )
}

fun viewportCenteredOnWorldPoint(
    currentViewport: GraphynViewport,
    canvasSize: IntSize,
    worldPoint: Offset,
): GraphynViewport {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return currentViewport
    val screenCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    return currentViewport.copy(offset = screenCenter - (worldPoint * currentViewport.scale))
}
