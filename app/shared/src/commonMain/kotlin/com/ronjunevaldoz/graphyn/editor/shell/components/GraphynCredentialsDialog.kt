package com.ronjunevaldoz.graphyn.editor.shell.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlinx.coroutines.launch

/**
 * Settings modal: pick an environment (dev/prod/…), edit its well-known service URLs/keys and any
 * custom key-values, and persist via [store]. The in-app alternative to environment variables.
 */
@Composable
internal fun GraphynCredentialsDialog(store: SettingsStore, onDismiss: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(12.dp)

    var settings by remember { mutableStateOf(GraphynSettings()) }
    var activeEnv by remember { mutableStateOf(GraphynSettings.DEFAULT_ENV) }
    var rows by remember { mutableStateOf(emptyList<EnvRow>()) }
    var newEnv by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        settings = store.load()
        activeEnv = settings.activeEnvironment
        rows = rowsForEnv(settings, activeEnv)
    }
    fun switchTo(env: String) {
        settings = foldRows(settings, activeEnv, rows)
        activeEnv = env
        rows = rowsForEnv(settings, env)
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).noRippleClick(onDismiss))
        Column(
            modifier = Modifier.align(Alignment.Center).size(width = 520.dp, height = 560.dp)
                .shadow(16.dp, shape).clip(shape).background(colors.panelBackground)
                .border(1.dp, colors.border, shape).padding(16.dp).noRippleClick {},
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                BasicText("Settings", style = type.nodeTitle.copy(color = colors.textPrimary))
                MiniButton("✕") { onDismiss() }
            }
            EnvSelectorRow(
                settings = settings,
                activeEnv = activeEnv,
                newEnv = newEnv,
                onNewEnv = { newEnv = it },
                onSwitch = { switchTo(it) },
                onAdd = {
                    settings = addEnv(foldRows(settings, activeEnv, rows), newEnv)
                    if (settings.activeEnvironment != activeEnv && newEnv.isNotBlank()) { activeEnv = newEnv.trim(); rows = rowsForEnv(settings, activeEnv) }
                    newEnv = ""
                },
                onRunPod = {
                    settings = addRunPodEnv(foldRows(settings, activeEnv, rows))
                    activeEnv = settings.activeEnvironment
                    rows = rowsForEnv(settings, activeEnv)
                },
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.forEachIndexed { i, r ->
                    ValueRow(
                        row = r,
                        onValue = { v -> rows = rows.toMutableList().also { it[i] = r.copy(value = v) } },
                        onKey = { k -> rows = rows.toMutableList().also { it[i] = r.copy(key = k) } },
                        onRemove = { rows = rows.filterIndexed { j, _ -> j != i } },
                    )
                }
                MiniButton("Add value") { rows = rows + EnvRow("", "", pinned = false) }
            }
            MiniButton("Save", filled = true, modifier = Modifier.fillMaxWidth()) {
                val toSave = foldRows(settings, activeEnv, rows).copy(activeEnvironment = activeEnv)
                scope.launch { store.save(toSave); onDismiss() }
            }
        }
    }
}

@Composable
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
