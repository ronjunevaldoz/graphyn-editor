package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntOffset
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GraphynAutoLayoutUiTest {
    @get:Rule
    val rule = createComposeRule()

    private val loggerSpec = NodeSpec(
        type = "logger",
        label = "Logger",
        inputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
        outputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType)),
    )

    private fun nodeSpecs() = DefaultNodeSpecRegistry().apply { register(loggerSpec) }

    // A diamond: root -> b, root -> c, b -> d, c -> d — the shape that used to make the shared
    // child d overlap whichever parent branch got laid out first.
    private fun diamondWorkflow() = WorkflowDefinition(
        id = "workflow-diamond",
        name = "Diamond",
        nodes = listOf(
            NodeRef(id = "root", type = "logger"),
            NodeRef(id = "b", type = "logger"),
            NodeRef(id = "c", type = "logger"),
            NodeRef(id = "d", type = "logger"),
        ),
        connections = listOf(
            ConnectionRef("root", "message", "b", "message"),
            ConnectionRef("root", "message", "c", "message"),
            ConnectionRef("b", "message", "d", "message"),
            ConnectionRef("c", "message", "d", "message"),
        ),
    )

    @Test
    fun clickingAutoLayoutButtonRepositionsStackedNodes() {
        val state = GraphynEditorState(diamondWorkflow(), nodeSpecs = nodeSpecs()).apply {
            // Stack every node at the same fallback point so the click is what moves them.
            setNodePosition("root", IntOffset(0, 0))
            setNodePosition("b", IntOffset(0, 0))
            setNodePosition("c", IntOffset(0, 0))
            setNodePosition("d", IntOffset(0, 0))
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("auto-layout-button").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePositionsByNodeId.values.toSet().size == 4
        }

        val positions = state.nodePositionsByNodeId
        assertNotEquals(positions.getValue("root"), positions.getValue("b"))
        assertNotEquals(positions.getValue("b"), positions.getValue("d"))
        assertNotEquals(positions.getValue("c"), positions.getValue("d"))
        // d depends on both b and c, so it must land to the right of both.
        assertTrue(positions.getValue("d").x > positions.getValue("b").x)
        assertTrue(positions.getValue("d").x > positions.getValue("c").x)
    }

    @Test
    fun autoLayoutedDiamondScreenshot() {
        val state = GraphynEditorState(diamondWorkflow(), nodeSpecs = nodeSpecs())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("auto-layout-button").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            state.nodePositionsByNodeId.values.toSet().size == 4
        }
        rule.waitForIdle()

        val options = RoborazziOptions(
            recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )
        rule.onRoot().captureRoboImage(roborazziOptions = options)
    }
}
