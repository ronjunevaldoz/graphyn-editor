package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlin.math.roundToInt

@Composable
internal fun GraphynNodePickerPopup(
    screenPosition: Offset,
    compatibleSpecs: List<NodePickerSuggestion>,
    onPick: (spec: NodePickerSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GraphynDs.colors
    val shape = RoundedCornerShape(8.dp)
    Popup(
        offset = IntOffset(screenPosition.x.roundToInt(), screenPosition.y.roundToInt()),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 160.dp, max = 260.dp)
                .testTag("node-picker-popup")
                .shadow(8.dp, shape)
                .background(colors.panelBackground, shape)
                .border(1.dp, colors.border, shape),
        ) {
            BasicText(
                text = "Add node",
                style = GraphynDs.type.labelSmall.copy(color = colors.textSecondary),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
            if (compatibleSpecs.isEmpty()) {
                BasicText(
                    text = "No compatible nodes",
                    modifier = Modifier.padding(12.dp),
                    style = GraphynDs.type.bodySmall.copy(color = colors.textSecondary),
                )
            } else {
                compatibleSpecs.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onPick(suggestion) },
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("node-picker-item-${suggestion.spec.type}"),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(suggestion.accentColor),
                            )
                            BasicText(
                                text = suggestion.spec.label,
                                style = GraphynDs.type.body.copy(color = colors.textPrimary),
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
