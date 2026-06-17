package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.roundToInt

@Composable
internal fun GraphynCanvasTelemetryOverlay(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.55f), shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = "Scale ${formatScale(state.viewport.scale)}x",
            style = GraphynDs.type.labelSmall.copy(color = Color.White),
        )
        BasicText(
            text = "Offset ${formatOffset(state.viewport.offset)}",
            style = GraphynDs.type.labelSmall.copy(color = Color.White),
        )
    }
}

@Composable
internal fun GraphynSmallOverlayButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = GraphynDs.colors
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = GraphynDs.type.labelSmall.copy(color = colors.accentForeground),
        )
    }
}

private fun formatScale(scale: Float): String = ((scale * 100).roundToInt() / 100f).toString()

private fun formatOffset(offset: Offset): String {
    val x = (offset.x * 10).roundToInt() / 10f
    val y = (offset.y * 10).roundToInt() / 10f
    return "(${x}, ${y})"
}
