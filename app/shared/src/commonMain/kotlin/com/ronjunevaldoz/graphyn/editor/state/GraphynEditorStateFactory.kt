package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

@Composable
fun rememberGraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
): GraphynEditorState = remember(initialWorkflow) {
    GraphynEditorState(initialWorkflow)
}
