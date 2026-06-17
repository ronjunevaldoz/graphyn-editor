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

    private fun twoNodeState() =
        GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-two-drag",
                name = "TwoDrag",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                    NodeRef(id = "logger-2", type = "logger"),
                ),
                connections = emptyList(),
            ),
        ).apply {
            setNodePosition("logger-1", IntOffset(30, 30))
            setNodePosition("logger-2", IntOffset(360, 30))
        }

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
    fun draggingTwoNodesMovesThemIndependently() {
        val state = twoNodeState()

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        val startPos1 = state.nodePosition("logger-1", 0)
        val startPos2 = state.nodePosition("logger-2", 1)

        // Drag node-1 right
        rule.onNodeWithText("logger-1").performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(20f, 0f)) }
            up()
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-1", 0).x > startPos1.x
        }

        val afterDrag1Pos1 = state.nodePosition("logger-1", 0)
        val afterDrag1Pos2 = state.nodePosition("logger-2", 1)

        assertTrue(afterDrag1Pos1.x > startPos1.x, "node-1 should have moved right")
        assertEquals(startPos2, afterDrag1Pos2, "node-2 should not have moved when dragging node-1")

        // Drag node-2 down
        rule.onNodeWithText("logger-2").performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(0f, 20f)) }
            up()
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-2", 1).y > startPos2.y
        }

        val finalPos1 = state.nodePosition("logger-1", 0)
        val finalPos2 = state.nodePosition("logger-2", 1)

        assertTrue(finalPos2.y > startPos2.y, "node-2 should have moved down")
        assertEquals(afterDrag1Pos1, finalPos1, "node-1 should not have moved when dragging node-2")
    }

    @Test
    fun twoNodesDraggedScreenshot() {
        val state = twoNodeState()

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        // Drag node-1 to the right and slightly down
        rule.onNodeWithText("logger-1").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(20f, 10f)) }
            up()
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-1", 0).x > 30
        }

        // Drag node-2 to the left and slightly down
        rule.onNodeWithText("logger-2").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(-20f, 10f)) }
            up()
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePosition("logger-2", 1).x < 360
        }

        rule.waitForIdle()
        rule.onRoot().captureRoboImage(roborazziOptions = roboOptions)
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
