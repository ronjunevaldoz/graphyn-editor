package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphynNodePositionPersistenceTest {

    @Test
    fun movingNodeUpdatesSerializableWorkflowLayout() {
        val state = GraphynEditorState(workflow())

        state.setNodePosition("node-1", IntOffset(320, 180))

        assertEquals(
            WorkflowNodePosition(320, 180),
            state.workflow?.nodePositions?.get("node-1"),
        )
    }

    @Test
    fun storedNodePositionIsRestoredOnEditorCreation() {
        val state = GraphynEditorState(
            workflow().copy(
                nodePositions = mapOf("node-1" to WorkflowNodePosition(420, 260)),
            ),
        )

        assertEquals(IntOffset(420, 260), state.nodePosition("node-1", 0))
    }

    @Test
    fun workflowWithoutStoredLayoutRemainsEligibleForAutoLayout() {
        val state = GraphynEditorState(workflow())

        assertTrue(state.nodePositionsByNodeId.isEmpty())
    }

    @Test
    fun firstDragStartsFromVisibleFallbackPosition() {
        val state = GraphynEditorState(workflow())
        val fallback = GraphynCanvasLayout.fallbackPosition(0)

        state.moveNode("node-1", IntOffset(12, 8))

        assertEquals(IntOffset(fallback.x + 12, fallback.y + 8), state.nodePosition("node-1", 0))
    }

    private fun workflow() = WorkflowDefinition(
        id = "workflow",
        name = "Workflow",
        nodes = listOf(NodeRef("node-1", "test")),
        connections = emptyList(),
    )
}
