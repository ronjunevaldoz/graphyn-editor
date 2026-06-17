package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntOffset
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphynNodeDragUiTest {
    @get:Rule
    val rule = createComposeRule()

    private val roboOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    )

    private fun singleNodeState(start: IntOffset = IntOffset(100, 120)) =
        GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-drag",
                name = "Drag",
                nodes = listOf(NodeRef(id = "logger-1", type = "logger")),
                connections = emptyList(),
            ),
        ).apply { setNodePosition("logger-1", start) }

    @Test
    fun draggingANodeDoesNotPanTheViewport() {
        val state = singleNodeState()

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
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

    @Test
    fun nodeDragPositionMatchesDeltaWithoutSlopJump() {
        val start = IntOffset(200, 200)
        val state = singleNodeState(start)

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        // Drag in small steps so we can verify smooth accumulation (no slop jump).
        rule.onNodeWithText("logger-1").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(10f, 0f)) }
            up()
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-1", 0).x > start.x
        }

        val finalX = state.nodePosition("logger-1", 0).x
        // After 10 × 10px steps (100px total), slop is consumed silently.
        // The node should have moved, but not further than the total drag distance.
        assertTrue(finalX > start.x, "Node should have moved right")
        assertTrue(finalX <= start.x + 100, "Node should not overshoot total drag (was: $finalX, max: ${start.x + 100})")
    }

    @Test
    fun nodeDragScreenshot() {
        val state = singleNodeState(IntOffset(40, 40))

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        rule.onNodeWithText("logger-1").performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(15f, 0f)) }
            up()
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-1", 0).x > 40
        }

        rule.waitForIdle()
        rule.onRoot().captureRoboImage(roborazziOptions = roboOptions)
    }
}
