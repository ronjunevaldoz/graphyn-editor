package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/**
 * Full-screen workflow launcher showing saved, recent, and bundled template workflows.
 *
 * Sits in front of [com.ronjunevaldoz.graphyn.editor.shell.GraphynSubgraphNavigator].
 * The host is responsible for switching from launcher to navigator when [onOpen] fires.
 *
 * @param templates Bundled starting points shown in the Templates section.
 * @param recentWorkflows In-session recently opened workflows shown above templates.
 * @param savedWorkflows Persisted workflows loaded from [com.ronjunevaldoz.graphyn.core.store.WorkflowStore].
 * @param onNew Called when the user wants a blank new workflow.
 * @param onOpenSaved Called with the workflow ID when the user picks a saved workflow.
 * @param onOpen Called with the selected template; host should load the workflow into the editor.
 */
@Composable
fun GraphynWorkflowLauncher(
    templates: List<WorkflowTemplate>,
    recentWorkflows: List<WorkflowTemplate> = emptyList(),
    savedWorkflows: List<WorkflowMeta> = emptyList(),
    onNew: () -> Unit = {},
    onOpenSaved: (String) -> Unit = {},
    onOpen: (WorkflowTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var selectedView by remember(savedWorkflows.isEmpty(), recentWorkflows.isEmpty()) {
        mutableStateOf(
            if (savedWorkflows.isEmpty() && recentWorkflows.isEmpty()) LauncherView.Templates
            else LauncherView.Workflows,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText("Graphyn", style = type.appTitle.copy(color = colors.textPrimary))
                    BasicText("Open a workflow or start from a template.", style = type.bodySmall.copy(color = colors.textSecondary))
                }
                NewWorkflowButton(onClick = onNew)
            }

            LauncherTabs(
                selected = selectedView,
                workflowCount = savedWorkflows.size + recentWorkflows.size,
                templateCount = templates.size,
                onSelect = { selectedView = it },
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (selectedView) {
                    LauncherView.Workflows -> {
                        if (savedWorkflows.isNotEmpty()) {
                            item {
                                LauncherSection(title = "Saved") {
                                    savedWorkflows.forEach { meta ->
                                        SavedWorkflowCard(meta = meta, modifier = Modifier.fillMaxWidth()) {
                                            onOpenSaved(meta.id)
                                        }
                                    }
                                }
                            }
                        }
                        if (recentWorkflows.isNotEmpty()) {
                            item {
                                LauncherSection(title = "Recent") {
                                    recentWorkflows.forEach { template ->
                                        WorkflowLauncherCard(template = template, modifier = Modifier.fillMaxWidth()) {
                                            onOpen(template)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LauncherView.Templates -> {
                        val grouped = WorkflowCategory.entries.mapNotNull { category ->
                            templates.filter { it.category == category }.takeIf { it.isNotEmpty() }?.let { category to it }
                        }
                        grouped.forEach { (category, inCategory) ->
                            item(key = category) {
                                LauncherSection(title = category.label) {
                                    inCategory.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            pair.forEach { template ->
                                                WorkflowLauncherCard(template = template, modifier = Modifier.weight(1f)) {
                                                    onOpen(template)
                                                }
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
