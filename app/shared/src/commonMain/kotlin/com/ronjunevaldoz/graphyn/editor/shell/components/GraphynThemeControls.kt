package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynThemeMode

@Composable
internal fun ThemeControls(appearanceState: GraphynAppearanceState) {
    var presetMenuOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ToolbarPill(appearanceState.selectedPreset.label) { presetMenuOpen = true }
        DropdownMenu(expanded = presetMenuOpen, onDismissRequest = { presetMenuOpen = false }) {
            appearanceState.presets.forEach { preset ->
                DropdownMenuItem(
                    text = { androidx.compose.foundation.text.BasicText(preset.label) },
                    onClick = {
                        appearanceState.selectPreset(preset.id)
                        presetMenuOpen = false
                    },
                )
            }
        }
        ToolbarIconPill(themeIcon(appearanceState.themeMode)) {
            appearanceState.updateThemeMode(if (appearanceState.themeMode == GraphynThemeMode.Light) GraphynThemeMode.Dark else GraphynThemeMode.Light)
        }
    }
}

private fun themeIcon(mode: GraphynThemeMode): String = when (mode) {
    GraphynThemeMode.Light -> "☀"
    GraphynThemeMode.Dark -> "☾"
    GraphynThemeMode.System -> "◐"
}
