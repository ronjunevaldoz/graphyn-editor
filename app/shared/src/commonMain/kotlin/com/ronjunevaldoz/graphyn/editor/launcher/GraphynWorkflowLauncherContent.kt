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
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun GraphynWorkflowLauncherContent(
    templates: List<WorkflowTemplate>,
    recentWorkflows: List<WorkflowTemplate>,
    savedWorkflows: List<WorkflowMeta>,
    nodeSpecs: List<NodeSpec>,
    onNew: () -> Unit,
    onOpenSaved: (String) -> Unit,
    onOpen: (WorkflowTemplate) -> Unit,
    modifier: Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var selectedView by remember(savedWorkflows.isEmpty(), recentWorkflows.isEmpty(), templates.isEmpty(), nodeSpecs.isEmpty()) {
        mutableStateOf(
            when {
                savedWorkflows.isEmpty() && recentWorkflows.isEmpty() && templates.isEmpty() && nodeSpecs.isNotEmpty() -> LauncherView.Schema
                savedWorkflows.isEmpty() && recentWorkflows.isEmpty() -> LauncherView.Templates
                else -> LauncherView.Workflows
            },
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.canvasBackground), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText("Graphyn", style = type.appTitle.copy(color = colors.textPrimary))
                    BasicText("Open a workflow or inspect the published schema.", style = type.bodySmall.copy(color = colors.textSecondary))
                }
                NewWorkflowButton(onClick = onNew)
            }
            LauncherTabs(
                selected = selectedView,
                workflowCount = savedWorkflows.size + recentWorkflows.size,
                templateCount = templates.size,
                schemaCount = nodeSpecs.size,
                onSelect = { selectedView = it },
            )
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                when (selectedView) {
                    LauncherView.Workflows -> {
                        if (savedWorkflows.isNotEmpty()) item { LauncherSection("Saved") { savedWorkflows.forEach { meta -> SavedWorkflowCard(meta, Modifier.fillMaxWidth()) { onOpenSaved(meta.id) } } } }
                        if (recentWorkflows.isNotEmpty()) item { LauncherSection("Recent") { recentWorkflows.forEach { template -> WorkflowLauncherCard(template, Modifier.fillMaxWidth()) { onOpen(template) } } } }
                    }
                    LauncherView.Templates -> {
                        WorkflowCategory.entries.mapNotNull { category ->
                            templates.filter { it.category == category }.takeIf { it.isNotEmpty() }?.let { category to it }
                        }.forEach { (category, items) ->
                            item(key = category) { LauncherSection(category.label) { items.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { pair.forEach { template -> WorkflowLauncherCard(template, Modifier.weight(1f)) { onOpen(template) } }; if (pair.size == 1) Spacer(Modifier.weight(1f)) } } } }
                        }
                    }
                    LauncherView.Schema -> {
                        item {
                            WorkflowSchemaView(nodeSpecs = nodeSpecs)
                        }
                    }
                }
            }
        }
    }
}
