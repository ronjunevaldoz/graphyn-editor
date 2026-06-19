package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/**
 * Full-screen workflow launcher showing recent workflows and bundled templates.
 *
 * Sits in front of [com.ronjunevaldoz.graphyn.editor.shell.GraphynSubgraphNavigator].
 * The host is responsible for switching from launcher to navigator when [onOpen] fires.
 *
 * @param templates Bundled starting points shown in the Templates section.
 * @param recentWorkflows In-session recently opened workflows shown above templates.
 * @param onOpen Called with the selected template; host should load the workflow into the editor.
 */
@Composable
fun GraphynWorkflowLauncher(
    templates: List<WorkflowTemplate>,
    recentWorkflows: List<WorkflowTemplate> = emptyList(),
    onOpen: (WorkflowTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText("Graphyn", style = type.appTitle.copy(color = colors.textPrimary))
                BasicText("Open a workflow or start from a template.", style = type.bodySmall.copy(color = colors.textSecondary))
            }

            if (recentWorkflows.isNotEmpty()) {
                LauncherSection(title = "Recent") {
                    recentWorkflows.take(4).forEach { template ->
                        WorkflowLauncherCard(template = template, modifier = Modifier.fillMaxWidth()) { onOpen(template) }
                    }
                }
            }

            LauncherSection(title = "Templates") {
                templates.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { template ->
                            WorkflowLauncherCard(template = template, modifier = Modifier.weight(1f)) { onOpen(template) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LauncherSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val type = GraphynDs.type
    val colors = GraphynDs.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(title.uppercase(), style = type.labelSmall.copy(color = colors.textDisabled))
        content()
    }
}
