package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynViewport
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Composable
fun GraphynCanvasBackdrop(
    modifier: Modifier = Modifier,
    viewport: GraphynViewport,
    dotColor: Color,
) {
    val backdropStart = GraphynDs.colors.surfaceCard.copy(alpha = 0.16f)
    val backdropEnd = GraphynDs.colors.panelBackground.copy(alpha = 0.06f)

    Canvas(modifier = modifier) {
        val minorSpacing = 28.dp.toPx()
        val majorSpacing = minorSpacing * 4f
        val screenTopLeft = viewport.screenToWorld(Offset.Zero)
        val screenBottomRight = viewport.screenToWorld(Offset(size.width, size.height))
        val worldLeft = min(screenTopLeft.x, screenBottomRight.x)
        val worldTop = min(screenTopLeft.y, screenBottomRight.y)
        val worldRight = max(screenTopLeft.x, screenBottomRight.x)
        val worldBottom = max(screenTopLeft.y, screenBottomRight.y)

        drawRect(brush = Brush.verticalGradient(colors = listOf(backdropStart, backdropEnd)))

        // Minor lines — subdivision, drawn first so major lines paint over them
        var worldX = floor(worldLeft / minorSpacing) * minorSpacing
        while (worldX <= worldRight + minorSpacing) {
            val screenX = viewport.worldToScreen(Offset(worldX, 0f)).x
            drawLine(dotColor.copy(alpha = 0.18f), Offset(screenX, 0f), Offset(screenX, size.height), strokeWidth = 1f)
            worldX += minorSpacing
        }
        var worldY = floor(worldTop / minorSpacing) * minorSpacing
        while (worldY <= worldBottom + minorSpacing) {
            val screenY = viewport.worldToScreen(Offset(0f, worldY)).y
            drawLine(dotColor.copy(alpha = 0.18f), Offset(0f, screenY), Offset(size.width, screenY), strokeWidth = 1f)
            worldY += minorSpacing
        }

        // Major lines — cell boundaries, painted on top
        var worldMajorX = floor(worldLeft / majorSpacing) * majorSpacing
        while (worldMajorX <= worldRight + majorSpacing) {
            val screenX = viewport.worldToScreen(Offset(worldMajorX, 0f)).x
            drawLine(dotColor.copy(alpha = 0.45f), Offset(screenX, 0f), Offset(screenX, size.height), strokeWidth = 1f)
            worldMajorX += majorSpacing
        }
        var worldMajorY = floor(worldTop / majorSpacing) * majorSpacing
        while (worldMajorY <= worldBottom + majorSpacing) {
            val screenY = viewport.worldToScreen(Offset(0f, worldMajorY)).y
            drawLine(dotColor.copy(alpha = 0.45f), Offset(0f, screenY), Offset(size.width, screenY), strokeWidth = 1f)
            worldMajorY += majorSpacing
        }
    }
}
