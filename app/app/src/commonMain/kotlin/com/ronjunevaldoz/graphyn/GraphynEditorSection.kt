package com.ronjunevaldoz.graphyn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.shell.GraphynSubgraphNavigator
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun GraphynEditorSection(
    wf: WorkflowDefinition,
    store: WorkflowStore?,
    canvasBounds: GraphynCanvasBounds,
    branding: GraphynBranding,
    dependencies: GraphynEditorShellDependencies,
    appearanceState: GraphynAppearanceState,
    onHome: () -> Unit,
) {
    val state = rememberGraphynEditorState(
        initialWorkflow = wf,
        canvasBounds = canvasBounds,
        store = store,
    )
    // Templates ship without positions; lay them out once the canvas is measured.
    // Guarded on empty positions so a stored/edited layout is never clobbered.
    LaunchedEffect(Unit) {
        snapshotFlow { state.canvasSize to state.hasCanvasCards }
            .first { (size, ready) -> size.width > 0 && size.height > 0 && ready }
        if (state.nodePositionsByNodeId.isEmpty()) {
            state.dispatch(GraphynEditorIntent.AutoLayout)
        }
    }
    GraphynSubgraphNavigator(
        branding = branding,
        dependencies = dependencies,
        appearanceState = appearanceState,
        state = state,
        onHome = onHome,
    )
}
