package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynThemeMode

@Composable
internal fun ThemeControls(appearanceState: GraphynAppearanceState) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(6.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        appearanceState.presets.forEach { preset ->
            val selected = preset.id == appearanceState.selectedPresetId
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .heightIn(min = 26.dp)
                    .clip(shape)
                    .background(if (selected) colors.accent else colors.surfaceCard)
                    .border(1.dp, if (selected) colors.accent else colors.border, shape)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        appearanceState.selectPreset(preset.id)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = preset.label,
                    style = type.labelSmall.copy(
                        color = if (selected) colors.accentForeground else colors.textSecondary,
                    ),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Row(
            modifier = Modifier.clip(shape).border(1.dp, colors.border, shape),
        ) {
            listOf(GraphynThemeMode.Light to "L", GraphynThemeMode.Dark to "D").forEach { (mode, label) ->
                val selected = appearanceState.themeMode == mode
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .heightIn(min = 26.dp)
                        .background(if (selected) colors.accent else colors.surfaceCard)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            appearanceState.updateThemeMode(mode)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = label,
                        style = type.labelSmall.copy(
                            color = if (selected) colors.accentForeground else colors.textSecondary,
                        ),
                    )
                }
            }
        }
    }
}
