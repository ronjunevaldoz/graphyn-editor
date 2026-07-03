package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphynViewportZoomFloorTest {
    @Test
    fun manualZoomOutUsesTheSameFloorAsFitToContent() {
        val state = GraphynEditorState(
            WorkflowDefinition(id = "zoom-floor", name = "ZoomFloor", nodes = emptyList(), connections = emptyList()),
        )

        state.viewport = GraphynViewport(scale = 1f, offset = Offset.Zero)
        state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, 0.01f, Offset.Zero))

        assertEquals(GraphynViewportState.MinFitScale, state.viewport.scale)
    }
}
