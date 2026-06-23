package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.shortcuts.EditorShortcutAction
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState

/** Toolbar entry point: a "⌨ Keys" button that opens the shortcut editor in a popup. */
@Composable
internal fun ShortcutsToolbarButton(shortcutState: GraphynShortcutState) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.panelBackground)
                .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                .clickable(interactionSource = interaction, indication = null) { open = !open }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("⌨ Keys", style = type.label.copy(color = colors.textSecondary))
        }
        if (open) {
            Popup(onDismissRequest = { open = false }) {
                Box(modifier = Modifier.padding(top = 44.dp)) {
                    GraphynShortcutsPanel(
                        shortcutState = shortcutState,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GraphynShortcutsPanel(
    shortcutState: GraphynShortcutState,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var recordingId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .width(320.dp)
            .background(colors.surfaceCard)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicText("Keyboard Shortcuts", style = type.nodeTitle.copy(color = colors.textPrimary))
            TextButton("Reset all") { shortcutState.resetAll(); message = null; recordingId = null }
        }
        message?.let { BasicText(it, style = type.bodySmall.copy(color = colors.danger), modifier = Modifier.padding(vertical = 4.dp)) }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border).padding(vertical = 8.dp))
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EditorShortcutAction.entries.forEach { action ->
                ShortcutRow(
                    action = action,
                    shortcutState = shortcutState,
                    recording = recordingId == action.id,
                    onStartRecording = { recordingId = action.id; message = null },
                    onResult = { msg -> recordingId = null; message = msg },
                )
            }
        }
    }
}

@Composable
private fun TextButton(label: String, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        BasicText(label, style = type.bodySmall.copy(color = colors.accent))
    }
}
