package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding

@Composable
internal fun GraphynTopToolbar(
    modifier: Modifier = Modifier,
    branding: GraphynBranding,
    appearanceState: GraphynAppearanceState,
    shortcutState: GraphynShortcutState,
    canRun: Boolean,
    onRun: () -> Unit,
    onAutoLayout: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    workflowName: String? = null,
    onToggleAi: (() -> Unit)? = null,
    aiActive: Boolean = false,
    onToggleSettings: (() -> Unit)? = null,
    settingsActive: Boolean = false,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(
        modifier = modifier.fillMaxWidth().height(52.dp).background(colors.panelBackground).border(1.dp, colors.border, RoundedCornerShape(0.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        onHome?.let { ToolbarPill("← Home", onClick = it) }
        branding.logo?.let { logo -> Image(painter = logo, contentDescription = branding.appName, modifier = Modifier.width(20.dp), contentScale = ContentScale.Fit) }
        androidx.compose.foundation.text.BasicText(text = workflowName ?: branding.appName, style = type.appTitle.copy(color = colors.textPrimary))
        Spacer(Modifier.weight(1f))
        onToggleAi?.let { ToolbarPill("✨ AI", selected = aiActive, onClick = it) }
        onToggleSettings?.let { ToolbarPill("⚙", selected = settingsActive, onClick = it) }
        ShortcutsToolbarButton(shortcutState = shortcutState)
        ThemeControls(appearanceState = appearanceState)
        onAutoLayout?.let { ToolbarPill("Auto Layout", onClick = it) }
        if (canRun) {
            ToolbarPill("Execute", filled = true, onClick = onRun)
            Spacer(Modifier.width(8.dp))
        }
    }
}
