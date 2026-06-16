package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.roundToInt

@Composable
internal fun GraphynCanvasTelemetryOverlay(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    GraphynChromePanel(
        modifier = modifier,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Canvas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Scale ${formatScale(state.viewport.scale)}x",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "Offset ${formatOffset(state.viewport.offset)}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = "Pan: drag canvas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphynSmallOverlayButton(
                    text = "Zoom +",
                    tag = "zoom-in-button",
                    onClick = {
                        state.dispatch(
                            GraphynEditorIntent.UpdateViewportTransform(
                                pan = Offset.Zero,
                                zoom = 1.15f,
                                focus = Offset.Zero,
                            ),
                        )
                        state.addDebugLog("Zoomed in to ${formatScale(state.viewport.scale)}x")
                    },
                )
                GraphynSmallOverlayButton(
                    text = "Zoom -",
                    tag = "zoom-out-button",
                    onClick = {
                        state.dispatch(
                            GraphynEditorIntent.UpdateViewportTransform(
                                pan = Offset.Zero,
                                zoom = 1f / 1.15f,
                                focus = Offset.Zero,
                            ),
                        )
                        state.addDebugLog("Zoomed out to ${formatScale(state.viewport.scale)}x")
                    },
                )
                GraphynSmallOverlayButton(
                    text = "Reset",
                    tag = "reset-viewport-button",
                    onClick = { state.resetViewport() },
                )
            }
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

