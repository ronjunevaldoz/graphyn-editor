package com.ronjunevaldoz.graphyn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/**
 * Scrim + modal asking the user whether to persist a newly-opened template to the workflow library.
 * Tapping the scrim calls [onDismiss] (cancel without opening).
 */
@Composable
internal fun TemplateSaveDialog(
    templateName: String,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(12.dp)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(colors.panelBackground)
                .border(1.dp, colors.border, shape)
                .padding(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText("Open \"$templateName\"", style = type.nodeTitle.copy(color = colors.textPrimary))
            BasicText(
                "Save this workflow to your library so changes are preserved?",
                style = type.bodySmall.copy(color = colors.textSecondary),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TemplateSaveButton("Open without saving", colors.textSecondary, filled = false, onClick = onSkip)
                TemplateSaveButton("Save to library", colors.accent, filled = true, onClick = onSave)
            }
        }
    }
}

@Composable
private fun TemplateSaveButton(
    label: String,
    color: Color,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (filled) color else Color.Transparent)
            .border(1.dp, if (filled) color else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        BasicText(label, style = type.bodySmall.copy(color = if (filled) Color.White else color))
    }
}
