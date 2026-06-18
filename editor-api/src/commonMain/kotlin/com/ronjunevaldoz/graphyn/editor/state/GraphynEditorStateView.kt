package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.flow.StateFlow

interface GraphynEditorStateView {
    val workflow: WorkflowDefinition?
    val workflowFlow: StateFlow<WorkflowDefinition?>
    val selectedNodeId: String?
}
