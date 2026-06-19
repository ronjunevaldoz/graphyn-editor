package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only projection of [GraphynEditorState] exposed to editor plugins and panels.
 *
 * Plugins should depend on this interface rather than the full mutable state to avoid
 * accidental mutations.
 */
interface GraphynEditorStateView {
    val workflow: WorkflowDefinition?
    val workflowFlow: StateFlow<WorkflowDefinition?>
    val selectedNodeId: String?
}
