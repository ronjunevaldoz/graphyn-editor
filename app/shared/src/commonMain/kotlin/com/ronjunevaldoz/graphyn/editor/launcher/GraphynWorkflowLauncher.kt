package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta

/**
 * Full-screen workflow launcher showing saved workflows, recent workflows, bundled templates,
 * and the published node schema catalog.
 */
@Composable
fun GraphynWorkflowLauncher(
    templates: List<WorkflowTemplate>,
    recentWorkflows: List<WorkflowTemplate> = emptyList(),
    savedWorkflows: List<WorkflowMeta> = emptyList(),
    nodeSpecs: List<NodeSpec> = emptyList(),
    onNew: () -> Unit = {},
    onOpenSaved: (String) -> Unit = {},
    onOpen: (WorkflowTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    GraphynWorkflowLauncherContent(
        templates = templates,
        recentWorkflows = recentWorkflows,
        savedWorkflows = savedWorkflows,
        nodeSpecs = nodeSpecs,
        onNew = onNew,
        onOpenSaved = onOpenSaved,
        onOpen = onOpen,
        modifier = modifier,
    )
}
