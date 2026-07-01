package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/**
 * Environment picker row for the settings dialog: a chip per environment, a field + ＋ to add a new
 * one, and a one-click **RunPod** preset that scaffolds a load-balancing endpoint environment.
 */
@Composable
internal fun EnvSelectorRow(
    settings: GraphynSettings,
    activeEnv: String,
    newEnv: String,
    onNewEnv: (String) -> Unit,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    onRunPod: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
        settings.environments.forEach { env -> EnvChip(env.name, env.name == activeEnv) { onSwitch(env.name) } }
        CredInput(newEnv, "new env", Modifier.width(88.dp), onNewEnv)
        MiniButton("＋", onClick = onAdd)
        MiniButton("RunPod", onClick = onRunPod)
    }
}

@Composable
private fun EnvChip(name: String, active: Boolean, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.clip(shape).background(if (active) colors.accent else colors.panelBackground)
            .border(1.dp, if (active) colors.accent else colors.border, shape)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(name, style = GraphynDs.type.label.copy(color = if (active) colors.accentForeground else colors.textSecondary))
    }
}
