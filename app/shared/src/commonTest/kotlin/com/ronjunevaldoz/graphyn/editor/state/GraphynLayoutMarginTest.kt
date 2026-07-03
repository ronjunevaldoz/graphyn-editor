package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasInset
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphynLayoutMarginTest {
    @Test
    fun fallbackPlacementStartsInsideTheCanvasInset() {
        assertEquals(IntOffset(GraphynCanvasInset, GraphynCanvasInset), GraphynCanvasLayout.fallbackPosition(0))
    }

    @Test
    fun autoLayoutKeepsNodesAwayFromTheWorldEdge() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "layout-margin",
                name = "LayoutMargin",
                nodes = listOf(
                    NodeRef("a", "test"),
                    NodeRef("b", "test"),
                ),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.AutoLayout)

        assertTrue(state.nodePositionsByNodeId.values.minOf { it.x } >= GraphynCanvasInset)
        assertTrue(state.nodePositionsByNodeId.values.minOf { it.y } >= GraphynCanvasInset)
    }
}
