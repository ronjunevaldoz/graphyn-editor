package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynLogPanel(
    modifier: Modifier = Modifier,
    state: GraphynEditorState,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.panelBackground)
            .border(width = 1.dp, color = colors.border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText("OUTPUT", style = type.panelTitle.copy(color = colors.textSecondary))
            val logs = state.debugLogEntries
            if (logs.isEmpty()) {
                BasicText("No log entries yet.", style = type.mono.copy(color = colors.textDisabled))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    logs.takeLast(6).forEach { entry ->
                        BasicText("> $entry", style = type.mono.copy(color = colors.textSecondary))
                    }
                }
            }
        }
        val result = state.lastExecutionResult
        if (result != null) {
            val nodeLabel: (String) -> String = { id ->
                state.workflow?.nodes?.firstOrNull { it.id == id }
                    ?.type?.substringAfterLast('.') ?: id
            }
            GraphynExecutionResultSection(
                result = result,
                statusByNodeId = state.executionStatusByNodeId,
                nodeLabel = nodeLabel,
                modifier = Modifier.fillMaxWidth()
                    .border(width = 1.dp, color = colors.border.copy(alpha = 0.5f)),
            )
        }
    }
}
