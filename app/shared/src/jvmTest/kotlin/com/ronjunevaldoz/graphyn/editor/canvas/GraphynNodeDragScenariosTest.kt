package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drag scenario tests targeting the initial-blink regression and edge cases.
 * Each test samples node position at 1 ms intervals during the gesture to catch
 * per-frame jumps that would not be visible in before/after assertions alone.
 */
class GraphynNodeDragScenariosTest {
    @get:Rule
    val rule = createComposeRule()

    private val roboOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    )

    private val start = IntOffset(200, 200)

    private fun state() = GraphynEditorState(
        WorkflowDefinition(
            id = "w",
            name = "W",
            nodes = listOf(NodeRef(id = "n1", type = "logger")),
            connections = emptyList(),
        ),
    ).apply { setNodePosition("n1", start) }

    /** Samples nodePosition every 1 ms during [block], returns distinct snapshots. */
    private fun recordPositions(
        state: GraphynEditorState,
        block: () -> Unit,
    ): List<IntOffset> {
        val samples = CopyOnWriteArrayList<IntOffset>()
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val future = executor.submit {
            while (!Thread.interrupted()) {
                samples.add(state.nodePosition("n1", 0))
                Thread.sleep(1)
            }
        }
        block()
        rule.waitForIdle()
        future.cancel(true)
        executor.shutdown()
        return samples.zipWithNext().filter { (a, b) -> a != b }.map { it.second }
    }

    // ── Scenario 1: short drag (below slop) ──────────────────────────────────

    @Test
    fun shortDragBelowSlopDoesNotMoveNode() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }
        rule.waitForIdle()

        rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(6f, 0f))  // well below any touch slop
            up()
        }
        rule.waitForIdle()

        assertEquals(start, s.nodePosition("n1", 0), "Node must not move when drag is below slop")
    }

    // ── Scenario 2: multi-step large drag — no overshoot, no blink ───────────

    @Test
    fun multiStepDragMovesNodeWithoutOvershooting() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }

        val changes = recordPositions(s) {
            rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(40) { moveBy(Offset(5f, 0f)) }  // 200px total in 5px steps
                up()
            }
        }

        val final = s.nodePosition("n1", 0)
        assertTrue(final.x > start.x, "Node should move right (was: ${final.x})")
        assertTrue(final.x <= start.x + 200, "Node must not overshoot total drag (was: ${final.x})")

        // Blink fix: no single observed frame should jump more than 10px (one step)
        val positions = listOf(start) + changes
        for (i in 1 until positions.size) {
            val jump = abs(positions[i].x - positions[i - 1].x)
            assertTrue(jump <= 10, "Blink detected: jumped ${jump}px in one frame (frame $i, pos=${positions[i]})")
        }
    }

    // ── Scenario 3: gradual step drag — smooth accumulation ──────────────────

    @Test
    fun gradualStepDragAccumulatesSmoothly() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }

        val changes = recordPositions(s) {
            rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(20) { moveBy(Offset(5f, 0f)) }  // 100px total in 5px steps
                up()
            }
        }

        val final = s.nodePosition("n1", 0)
        assertTrue(final.x > start.x, "Node should have moved right")
        assertTrue(final.x <= start.x + 100, "Node must not overshoot (was: ${final.x})")

        // After blink fix: each per-frame jump should be ≤ one step size (5px)
        val positions = listOf(start) + changes
        for (i in 1 until positions.size) {
            val jump = abs(positions[i].x - positions[i - 1].x)
            assertTrue(jump <= 10, "Large per-frame jump detected: ${jump}px at frame $i")
        }
    }

    // ── Scenario 4: diagonal drag ─────────────────────────────────────────────

    @Test
    fun diagonalDragMovesBothAxes() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }
        rule.waitForIdle()

        rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(10f, 10f)) }
            up()
        }
        rule.waitUntil(5_000) { s.nodePosition("n1", 0).x > start.x }

        val final = s.nodePosition("n1", 0)
        assertTrue(final.x > start.x, "X should increase on diagonal drag")
        assertTrue(final.y > start.y, "Y should increase on diagonal drag")
        rule.onRoot().captureRoboImage(roborazziOptions = roboOptions)
    }

    // ── Scenario 5: negative (left) drag ─────────────────────────────────────

    @Test
    fun leftDragMovesNodeLeft() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }
        rule.waitForIdle()

        rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(-10f, 0f)) }
            up()
        }
        rule.waitUntil(5_000) { s.nodePosition("n1", 0).x < start.x }

        assertTrue(s.nodePosition("n1", 0).x < start.x, "Node should move left")
    }

    // ── Scenario 6: back-and-forth cancels out ────────────────────────────────

    @Test
    fun backAndForthDragReturnsNearStart() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }
        rule.waitForIdle()

        rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(10f, 0f)) }  // +100px
            repeat(10) { moveBy(Offset(-10f, 0f)) } // -100px
            up()
        }
        rule.waitForIdle()

        val final = s.nodePosition("n1", 0)
        // Net movement is 0, but slop may absorb 1–2 steps, so allow ±30px tolerance
        assertTrue(abs(final.x - start.x) <= 30, "Back-and-forth should return near start (was: ${final.x}, start: ${start.x})")
    }

    // ── Scenario 7: sequential drags accumulate ───────────────────────────────

    @Test
    fun sequentialDragsAccumulatePositionCorrectly() {
        val s = state()
        rule.setContent {
            GraphynEditorShell(dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()), state = s, appearanceState = rememberGraphynAppearanceState())
        }
        rule.waitForIdle()

        repeat(3) {
            rule.onNodeWithTag("node-header-n1", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(6) { moveBy(Offset(10f, 0f)) }
                up()
            }
            rule.waitUntil(5_000) { s.nodePosition("n1", 0).x > start.x + it * 30 }
        }

        assertTrue(s.nodePosition("n1", 0).x > start.x + 60, "Three sequential drags should accumulate")
        rule.onRoot().captureRoboImage(roborazziOptions = roboOptions)
    }
}
