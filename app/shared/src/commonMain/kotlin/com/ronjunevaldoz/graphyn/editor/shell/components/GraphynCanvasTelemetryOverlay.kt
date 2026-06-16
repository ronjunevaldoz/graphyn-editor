package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.roundToInt

/**
 * TODO audit for design system
 * The current design is for stats
 */
@Composable
internal fun GraphynCanvasTelemetryOverlay(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Scale ${formatScale(state.viewport.scale)}x",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = "Offset ${formatOffset(state.viewport.offset)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
internal fun GraphynSmallOverlayButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .height(30.dp)
            .testTag(tag),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun formatScale(scale: Float): String = ((scale * 100).roundToInt() / 100f).toString()

private fun formatOffset(offset: Offset): String {
    val x = (offset.x * 10).roundToInt() / 10f
    val y = (offset.y * 10).roundToInt() / 10f
    return "(${x}, ${y})"
}

