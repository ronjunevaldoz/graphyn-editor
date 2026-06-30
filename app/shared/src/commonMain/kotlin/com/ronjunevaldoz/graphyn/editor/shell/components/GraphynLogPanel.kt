package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

private enum class OutputTab { Output, Logs, Debug }

@Composable
internal fun GraphynLogPanel(
    modifier: Modifier = Modifier,
    state: GraphynEditorState,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var expanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(OutputTab.Output) }

    val result = state.lastExecutionResult

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.panelBackground)
            .border(width = 1.dp, color = colors.border),
    ) {
        // Header row
        val headerInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(interactionSource = headerInteraction, indication = null) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutputTab.entries.forEach { tab ->
                    val isActive = expanded && tab == activeTab
                    val tabInteraction = remember { MutableInteractionSource() }
                    BasicText(
                        tab.name.uppercase(),
                        modifier = Modifier.clickable(interactionSource = tabInteraction, indication = null) {
                            if (!expanded || activeTab != tab) { expanded = true; activeTab = tab }
                            else expanded = false
                        },
                        style = type.panelTitle.copy(
                            color = if (isActive) colors.accent else colors.textSecondary,
                        ),
                    )
                }
            }
            BasicText(if (expanded) "▼" else "▲", style = type.bodySmall.copy(color = colors.textDisabled))
        }

        // Panel body
        AnimatedVisibility(visible = expanded) {
            val scrollState = rememberScrollState()
            SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (activeTab) {
                    OutputTab.Output -> {
                        if (result == null) {
                            BasicText(
                                "No output yet. Press Run ▶ to execute the workflow.",
                                style = type.mono.copy(color = colors.textDisabled),
                            )
                        } else {
                            val nodeLabel: (String) -> String = { id ->
                                state.workflow?.nodes?.firstOrNull { it.id == id }
                                    ?.type?.substringAfterLast('.') ?: id
                            }
                            GraphynExecutionResultSection(
                                result = result,
                                statusByNodeId = state.executionStatusByNodeId,
                                nodeLabel = nodeLabel,
                            )
                        }
                    }
                    OutputTab.Logs -> {
                        val logs = state.debugLogEntries
                        if (logs.isEmpty()) {
                            BasicText("No log entries yet.", style = type.mono.copy(color = colors.textDisabled))
                        } else {
                            logs.takeLast(20).forEach { entry ->
                                BasicText("> $entry", style = type.mono.copy(color = colors.textSecondary))
                            }
                        }
                    }
                    OutputTab.Debug -> {
                        val telemetry = state.telemetryEntries
                        if (telemetry.isEmpty()) {
                            BasicText("No telemetry yet. Trigger Auto Layout or resize the window.", style = type.mono.copy(color = colors.textDisabled))
                        } else {
                            telemetry.takeLast(20).forEach { entry ->
                                BasicText("> $entry", style = type.mono.copy(color = colors.textSecondary))
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
