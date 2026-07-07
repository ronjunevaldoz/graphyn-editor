package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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

class GraphynAutoLayoutScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private val loggerSpec = NodeSpec(
        type = "logger",
        label = "Logger",
        inputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
        outputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType)),
    )

    private fun nodeSpecs() = DefaultNodeSpecRegistry().apply { register(loggerSpec) }

    // c waits for both parents so BFS enqueues d first — a→c must then cross b→d.
    // The barycenter sweep reorders the column and removes the crossing.
    private fun crossingWorkflow() = WorkflowDefinition(
        id = "workflow-crossing",
        name = "Crossing",
        nodes = listOf("a", "b", "c", "d").map { NodeRef(id = it, type = "logger") },
        connections = listOf(
            ConnectionRef("b", "message", "d", "message"),
            ConnectionRef("a", "message", "c", "message"),
            ConnectionRef("b", "message", "c", "message"),
        ),
    )

    private fun capture(buttonTag: String) {
        val state = GraphynEditorState(crossingWorkflow(), nodeSpecs = nodeSpecs())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag(buttonTag).performClick()
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

    @Test
    fun bfsOrderLayoutScreenshot() = capture("auto-layout-bfs-button")

    @Test
    fun crossingMinimizedLayoutScreenshot() = capture("auto-layout-button")
}
