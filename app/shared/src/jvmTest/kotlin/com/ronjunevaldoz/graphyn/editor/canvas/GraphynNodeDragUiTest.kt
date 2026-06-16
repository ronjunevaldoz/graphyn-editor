package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphynNodeDragUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun draggingANodeDoesNotPanTheViewport() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-drag",
                name = "Drag",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
        ).apply {
            setNodePosition("logger-1", IntOffset(100, 120))
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(
                    nodeSpecs = DefaultNodeSpecRegistry(),
                ),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        rule.onNodeWithText("logger-1").performTouchInput {
            down(center)
            moveBy(Offset(120f, 0f))
            up()
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-1", 0) != IntOffset(100, 120)
        }

        assertEquals(Offset.Zero, state.viewport.offset)
        assertTrue(state.nodePosition("logger-1", 0).x > 100)
    }
}
