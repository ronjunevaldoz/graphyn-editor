package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
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
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GraphynCanvasSurfaceUiTest {
    @get:Rule
    val rule = createComposeRule()

    private val loggerSpec = NodeSpec(
        type = "logger",
        label = "Logger",
        inputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
        outputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType)),
    )

    private fun nodeSpecs() = DefaultNodeSpecRegistry().apply { register(loggerSpec) }

    private fun twoNodeWorkflow(connection: ConnectionRef? = null) = WorkflowDefinition(
        id = "workflow-canvas",
        name = "Canvas",
        nodes = listOf(
            NodeRef(id = "logger-1", type = "logger"),
            NodeRef(id = "logger-2", type = "logger"),
        ),
        connections = listOfNotNull(connection),
    )

    @Test
    fun outputPortDotExistsForNodeWithSpec() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("output-port-logger-1-message").assertIsDisplayed()
        rule.onNodeWithTag("output-port-logger-2-message").assertIsDisplayed()
    }

    @Test
    fun inputPortDotExistsForNodeWithSpec() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("input-port-logger-1-message").assertIsDisplayed()
        rule.onNodeWithTag("input-port-logger-2-message").assertIsDisplayed()
    }

    @Test
    fun clickingOutputPortStartsConnectionDraft() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        assertNull(state.connectionDraft)
        rule.onNodeWithTag("output-port-logger-1-message").performClick()

        rule.waitUntil(timeoutMillis = 5_000) { state.connectionDraft != null }

        assertEquals("logger-1", state.connectionDraft?.fromNodeId)
        assertEquals("message", state.connectionDraft?.fromPort)
    }

    @Test
    fun connectionMidpointDotExistsWhenConnectionIsPresent() {
        val connection = ConnectionRef(
            fromNodeId = "logger-1",
            fromPort = "message",
            toNodeId = "logger-2",
            toPort = "message",
        )
        val state = GraphynEditorState(twoNodeWorkflow(connection)).apply {
            setNodePosition("logger-1", IntOffset(0, 0))
            setNodePosition("logger-2", IntOffset(400, 0))
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("connection-midpoint-logger-1-message").assertIsDisplayed()
    }

    @Test
    fun clickingConnectionMidpointSelectsConnection() {
        val connection = ConnectionRef(
            fromNodeId = "logger-1",
            fromPort = "message",
            toNodeId = "logger-2",
            toPort = "message",
        )
        val state = GraphynEditorState(twoNodeWorkflow(connection)).apply {
            setNodePosition("logger-1", IntOffset(0, 0))
            setNodePosition("logger-2", IntOffset(400, 0))
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        assertNull(state.selectedConnection)
        rule.onNodeWithTag("connection-midpoint-logger-1-message").performClick()

        rule.waitUntil(timeoutMillis = 5_000) { state.selectedConnection != null }

        assertEquals(connection, state.selectedConnection)
    }

    @Test
    fun clickingEmptyCanvasWhileDraftActiveShowsNodePicker() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("output-port-logger-1-message").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.connectionDraft != null }

        rule.onNodeWithTag("canvas-background").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.nodePickerState != null }

        assertNotNull(state.nodePickerState)
        // draft is preserved so the picker can complete the connection
        assertNotNull(state.connectionDraft)
    }

    @Test
    fun clickingInputPortWithNoDraftStartsInputDraft() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        assertNull(state.connectionDraft)
        rule.onNodeWithTag("input-port-logger-2-message").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.connectionDraft != null }

        assertEquals("logger-2", state.connectionDraft?.fromNodeId)
        assertEquals("message", state.connectionDraft?.fromPort)
        assertEquals(true, state.connectionDraft?.isFromInput)
    }

    @Test
    fun emptyNodesHintShownWhenWorkflowHasNoNodes() {
        val state = GraphynEditorState(
            WorkflowDefinition(id = "empty", name = "Empty", nodes = emptyList(), connections = emptyList()),
        )

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("empty-nodes-hint").assertIsDisplayed()
    }

    @Test
    fun reconnectingSelectedConnectionUpdatesTarget() {
        val connection = ConnectionRef(
            fromNodeId = "logger-1",
            fromPort = "message",
            toNodeId = "logger-2",
            toPort = "message",
        )
        val state = GraphynEditorState(twoNodeWorkflow(connection)).apply {
            setNodePosition("logger-1", IntOffset(0, 0))
            setNodePosition("logger-2", IntOffset(400, 0))
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        // select the connection via its midpoint dot
        rule.onNodeWithTag("connection-midpoint-logger-1-message").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.selectedConnection != null }

        // click the input port of logger-1 to reconnect the same connection back to logger-1
        rule.onNodeWithTag("input-port-logger-1-message").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            state.selectedConnection?.toNodeId == "logger-1"
        }

        assertEquals("logger-1", state.selectedConnection?.toNodeId)
        assertEquals("message", state.selectedConnection?.toPort)
        assertEquals("logger-1", state.workflow?.connections?.first()?.toNodeId)
    }

    @Test
    fun nodePickerPopupAppearsWhenShowNodePickerDispatched() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("output-port-logger-1-message").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.connectionDraft != null }

        state.dispatch(GraphynEditorIntent.ShowNodePicker(
            screenPosition = Offset(200f, 200f),
            worldPosition = Offset(100f, 100f),
        ))
        rule.waitUntil(timeoutMillis = 5_000) { state.nodePickerState != null }

        rule.onNodeWithTag("node-picker-popup").assertIsDisplayed()
    }

    @Test
    fun pickingNodeFromPickerAddsNodeAndConnects() {
        val state = GraphynEditorState(twoNodeWorkflow())

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("output-port-logger-1-message").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.connectionDraft != null }

        state.dispatch(GraphynEditorIntent.ShowNodePicker(
            screenPosition = Offset(200f, 200f),
            worldPosition = Offset(100f, 100f),
        ))
        rule.waitUntil(timeoutMillis = 5_000) { state.nodePickerState != null }

        rule.onNodeWithTag("node-picker-item-logger").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { state.nodePickerState == null }

        assertEquals(3, state.workflow?.nodes?.size)
        assertNotNull(state.workflow?.connections?.firstOrNull())
    }

    @Test
    fun portDotsAndConnectionMidpointScreenshot() {
        val connection = ConnectionRef(
            fromNodeId = "logger-1",
            fromPort = "message",
            toNodeId = "logger-2",
            toPort = "message",
        )
        val state = GraphynEditorState(twoNodeWorkflow(connection)).apply {
            setNodePosition("logger-1", IntOffset(20, 20))
            setNodePosition("logger-2", IntOffset(400, 20))
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = nodeSpecs()),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        val options = RoborazziOptions(
            recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )
        rule.onRoot().captureRoboImage(roborazziOptions = options)
    }
}
