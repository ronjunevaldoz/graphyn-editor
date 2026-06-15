package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds

@Composable
fun rememberGraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
): GraphynEditorState = remember(initialWorkflow, canvasBounds) {
    GraphynEditorState(
        initialWorkflow = initialWorkflow,
        canvasBounds = canvasBounds,
    )
}
