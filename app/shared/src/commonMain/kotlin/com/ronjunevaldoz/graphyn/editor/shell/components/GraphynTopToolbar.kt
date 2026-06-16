package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynThemeMode

@Composable
internal fun GraphynTopToolbar(
    modifier: Modifier = Modifier,
    branding: GraphynBranding,
    appearanceState: GraphynAppearanceState,
    canRun: Boolean,
    onRun: () -> Unit,
) {
    GraphynChromePanel(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                branding.logo?.let { logo ->
                    Image(
                        painter = logo,
                        contentDescription = branding.appName,
                        modifier = Modifier.height(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = branding.appName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canRun) {
                    Button(onClick = onRun) {
                        Text("Run")
                    }
                }
            }
            GraphynThemeControls(appearanceState = appearanceState)
        }
    }
}

@Composable
internal fun GraphynThemeControls(
    appearanceState: GraphynAppearanceState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GraphynThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appearanceState.themeMode == mode,
                    onClick = { appearanceState.updateThemeMode(mode) },
                    label = {
                        Text(
                            text = when (mode) {
                                GraphynThemeMode.System -> "System"
                                GraphynThemeMode.Light -> "Light"
                                GraphynThemeMode.Dark -> "Dark"
                            },
                        )
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            appearanceState.presets.forEach { preset ->
                FilterChip(
                    selected = appearanceState.selectedPresetId == preset.id,
                    onClick = { appearanceState.selectPreset(preset.id) },
                    label = { Text(preset.label) },
                )
            }
        }
    }
}

