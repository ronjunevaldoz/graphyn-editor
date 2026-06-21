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
    /** The workflow currently loaded in the editor, or null before any workflow is opened. */
    val workflow: WorkflowDefinition?

    /** Hot [StateFlow] that emits whenever [workflow] changes. */
    val workflowFlow: StateFlow<WorkflowDefinition?>

    /** Id of the currently selected node, or null if nothing is selected. */
    val selectedNodeId: String?
}
