package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds

@Composable
fun rememberGraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
    nodeSpecs: NodeSpecRegistry? = null,
): GraphynEditorState = remember(initialWorkflow, canvasBounds, nodeSpecs) {
    GraphynEditorState(
        initialWorkflow = initialWorkflow,
        canvasBounds = canvasBounds,
        nodeSpecs = nodeSpecs,
    )
}
