package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.shortcuts.EditorShortcutAction
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState

@Composable
internal fun GraphynShortcutsPanel(
    shortcutState: GraphynShortcutState,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type

    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .background(colors.surfaceCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText("Keyboard Shortcuts", style = type.nodeTitle.copy(color = colors.textPrimary))
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border)
                .padding(vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EditorShortcutAction.entries.forEach { action ->
                ShortcutRow(action = action, shortcutState = shortcutState)
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    action: EditorShortcutAction,
    shortcutState: GraphynShortcutState,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val chord = shortcutState.chordFor(action)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(action.label, style = type.bodySmall.copy(color = colors.textPrimary))
        BasicText(chord.display(), style = type.mono.copy(color = colors.textSecondary))
    }
}
