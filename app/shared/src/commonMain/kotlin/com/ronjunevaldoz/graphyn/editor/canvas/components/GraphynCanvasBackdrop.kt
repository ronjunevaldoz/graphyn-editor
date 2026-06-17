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
        val worldSpacing = 28.dp.toPx()
        val majorWorldSpacing = worldSpacing * 4f
        val dotRadius = (1.15.dp.toPx() + (1f / viewport.scale) * 0.35f).coerceIn(1.15f, 2.6f)
        val screenTopLeft = viewport.screenToWorld(Offset.Zero)
        val screenBottomRight = viewport.screenToWorld(Offset(size.width, size.height))
        val worldLeft = min(screenTopLeft.x, screenBottomRight.x)
        val worldTop = min(screenTopLeft.y, screenBottomRight.y)
        val worldRight = max(screenTopLeft.x, screenBottomRight.x)
        val worldBottom = max(screenTopLeft.y, screenBottomRight.y)

        val startX = floor(worldLeft / worldSpacing) * worldSpacing
        val startY = floor(worldTop / worldSpacing) * worldSpacing
        val majorStartX = floor(worldLeft / majorWorldSpacing) * majorWorldSpacing
        val majorStartY = floor(worldTop / majorWorldSpacing) * majorWorldSpacing

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(backdropStart, backdropEnd),
            ),
        )

        var columnIndex = 0
        var worldX = startX
        while (worldX <= worldRight + worldSpacing) {
            var rowIndex = 0
            var worldY = startY
            while (worldY <= worldBottom + worldSpacing) {
                val screenPoint = viewport.worldToScreen(Offset(worldX, worldY))
                val isMajor = columnIndex % 4 == 0 && rowIndex % 4 == 0
                drawCircle(
                    color = dotColor.copy(alpha = if (isMajor) 0.75f else 0.35f),
                    radius = if (isMajor) dotRadius * 1.35f else dotRadius,
                    center = screenPoint,
                )
                worldY += worldSpacing
                rowIndex += 1
            }
            worldX += worldSpacing
            columnIndex += 1
        }

        var worldMajorX = majorStartX
        while (worldMajorX <= worldRight + majorWorldSpacing) {
            val screenMajorX = viewport.worldToScreen(Offset(worldMajorX, 0f)).x
            drawLine(
                color = dotColor.copy(alpha = 0.05f),
                start = Offset(screenMajorX, 0f),
                end = Offset(screenMajorX, size.height),
                strokeWidth = 1f,
            )
            worldMajorX += majorWorldSpacing
        }

        var worldMajorY = majorStartY
        while (worldMajorY <= worldBottom + majorWorldSpacing) {
            val screenMajorY = viewport.worldToScreen(Offset(0f, worldMajorY)).y
            drawLine(
                color = dotColor.copy(alpha = 0.05f),
                start = Offset(0f, screenMajorY),
                end = Offset(size.width, screenMajorY),
                strokeWidth = 1f,
            )
            worldMajorY += majorWorldSpacing
        }
    }
}
