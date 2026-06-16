package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.state.GraphynViewport

@Composable
fun GraphynCanvasBackdrop(
    modifier: Modifier = Modifier,
    viewport: GraphynViewport,
    dotColor: Color,
) {
    val backdropStart = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
    val backdropEnd = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)

    Canvas(modifier = modifier) {
        val baseSpacing = 28.dp.toPx()
        val spacing = (baseSpacing / viewport.scale).coerceAtLeast(10f)
        val dotRadius = (1.15.dp.toPx() + (1f / viewport.scale) * 0.35f).coerceIn(1.15f, 2.6f)
        val majorSpacing = spacing * 4f
        val originX = ((viewport.offset.x / viewport.scale) % spacing + spacing) % spacing
        val originY = ((viewport.offset.y / viewport.scale) % spacing + spacing) % spacing

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(backdropStart, backdropEnd),
            ),
        )

        var x = originX
        while (x < size.width) {
            var y = originY
            while (y < size.height) {
                val majorColumn = (((x - originX) / spacing).toInt()) % 4 == 0
                val majorRow = (((y - originY) / spacing).toInt()) % 4 == 0
                val isMajor = majorColumn && majorRow
                drawCircle(
                    color = dotColor.copy(alpha = if (isMajor) 0.75f else 0.35f),
                    radius = if (isMajor) dotRadius * 1.35f else dotRadius,
                    center = Offset(x, y),
                )
                y += spacing
            }
            x += spacing
        }

        var majorX = ((viewport.offset.x / viewport.scale) % majorSpacing + majorSpacing) % majorSpacing
        while (majorX < size.width) {
            drawLine(
                color = dotColor.copy(alpha = 0.05f),
                start = Offset(majorX, 0f),
                end = Offset(majorX, size.height),
                strokeWidth = 1f,
            )
            majorX += majorSpacing
        }
    }
}
