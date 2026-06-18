@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.execute
import com.ronjunevaldoz.graphyn.editor.state.updateNodeOutputs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorRegistryTest {
    @Test
    fun defaultEditorPanelRegistryStoresRegisteredPanels() {
        val registry = DefaultEditorPanelRegistry()

        registry.register(
            "printer",
            EditorPanelFactory { _: EditorPanelContext -> },
        )

        assertNotNull(registry.resolve("printer"))
    }

    @Test
    fun editorPanelsCanBeRegisteredAndResolved() {
        val registry = DefaultGraphynEditorPluginRegistry()
        registry.install(
            object : GraphynEditorPlugin {
                override val metadata = GraphynEditorPluginMetadata(
                    id = "graphyn.test.editor",
                    displayName = "Editor Test",
                    version = "1.0.0",
                )

                override fun register(registrar: GraphynEditorPluginRegistrar) {
                    registrar.registerPanel(
                        "printer",
                        EditorPanelFactory { _: EditorPanelContext -> },
                    )
                }
            },
        )

        assertNotNull(registry.panels.resolve("printer"))
    }

    @Test
    fun editorStateTracksOutputsAndDownstreamImpact() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-1",
                name = "Editor",
                nodes = listOf(
                    NodeRef(id = "switch-1", type = "switch"),
                    NodeRef(id = "panel-1", type = "panel"),
                ),
                connections = listOf(
                    ConnectionRef(
                        fromNodeId = "switch-1",
                        fromPort = "on",
                        toNodeId = "panel-1",
                        toPort = "enabled",
                    ),
                ),
            ),
        )

        state.updateNodeOutputs(
            "switch-1",
            mapOf("on" to WorkflowValue.BooleanValue(true)),
        )

        assertEquals(
            WorkflowValue.BooleanValue(true),
            state.outputsFor("switch-1")["on"],
        )
        assertEquals(
            WorkflowValue.BooleanValue(true),
            state.flattenedOutputsFor("switch-1")["on"],
        )
        assertTrue("panel-1" in state.affectedNodeIds("switch-1"))
    }

    @Test
    fun editorStateTracksNodePositionsAndFallbackLayout() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-layout",
                name = "Layout",
                nodes = listOf(
                    NodeRef(id = "first", type = "switch"),
                    NodeRef(id = "second", type = "display"),
                ),
                connections = emptyList(),
            ),
        )

        assertEquals(IntOffset.Zero, state.nodePosition("first", 0))
        assertEquals(IntOffset(304, 0), state.nodePosition("second", 1))

        state.setNodePosition("first", IntOffset(120, 80))
        state.dispatch(GraphynEditorIntent.MoveNode("first", IntOffset(10, 20)))

        assertEquals(IntOffset(130, 100), state.nodePosition("first", 0))
    }

    @Test
    fun editorStateCompletesDraftConnectionsIntoWorkflowConnections() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-connect",
                name = "Connect",
                nodes = listOf(
                    NodeRef(id = "source", type = "switch"),
                    NodeRef(id = "target", type = "display"),
                ),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.BeginConnection("source", "on"))
        state.dispatch(GraphynEditorIntent.CompleteConnection("target", "enabled"))

        assertEquals(
            listOf(
                ConnectionRef(
                    fromNodeId = "source",
                    fromPort = "on",
                    toNodeId = "target",
                    toPort = "enabled",
                ),
            ),
            state.workflow?.connections,
        )
        assertEquals(null, state.connectionDraft)
    }

    @Test
    fun editorStateDeletesSelectedConnection() {
        val connection = ConnectionRef(
            fromNodeId = "source",
            fromPort = "on",
            toNodeId = "target",
            toPort = "enabled",
        )
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-delete-conn",
                name = "DeleteConn",
                nodes = listOf(
                    NodeRef(id = "source", type = "switch"),
                    NodeRef(id = "target", type = "display"),
                ),
                connections = listOf(connection),
            ),
        )

        state.dispatch(GraphynEditorIntent.SelectConnection(connection))
        assertEquals(connection, state.selectedConnection)

        state.dispatch(GraphynEditorIntent.DeleteSelectedConnection)

        assertNull(state.selectedConnection)
        assertEquals(emptyList(), state.workflow?.connections)
    }

    @Test
    fun beginConnectionFromInputPortSetsDraftIsFromInput() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(NodeRef(id = "n1", type = "switch")),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.BeginConnection("n1", "enabled", isFromInput = true))

        val draft = state.connectionDraft
        assertNotNull(draft)
        assertEquals("n1", draft!!.fromNodeId)
        assertEquals("enabled", draft.fromPort)
        assertEquals(true, draft.isFromInput)
    }

    @Test
    fun completeConnectionFromInputSwapsEndpointOrder() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(
                    NodeRef(id = "source", type = "switch"),
                    NodeRef(id = "target", type = "display"),
                ),
                connections = emptyList(),
            ),
        )

        // Start from input port of "target"
        state.dispatch(GraphynEditorIntent.BeginConnection("target", "enabled", isFromInput = true))
        // Complete on output port of "source"
        state.dispatch(GraphynEditorIntent.CompleteConnection("source", "on"))

        val conn = state.workflow?.connections?.firstOrNull()
        assertNotNull(conn)
        assertEquals("source", conn!!.fromNodeId)
        assertEquals("on", conn.fromPort)
        assertEquals("target", conn.toNodeId)
        assertEquals("enabled", conn.toPort)
    }

    @Test
    fun cancelConnectionOnEmptyCanvasDispatchCancelsActiveDraft() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(NodeRef(id = "n1", type = "switch")),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.BeginConnection("n1", "on"))
        assertNotNull(state.connectionDraft)

        state.dispatch(GraphynEditorIntent.CancelConnection)

        assertNull(state.connectionDraft)
        assertNull(state.connectionDraftPosition)
    }

    @Test
    fun deleteKeyDeletesSelectedNode() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(NodeRef(id = "n1", type = "switch")),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.SelectNode("n1"))
        assertNotNull(state.selectedNodeId)

        state.dispatch(GraphynEditorIntent.DeleteSelectedNode)

        assertNull(state.selectedNodeId)
        assertTrue(state.workflow?.nodes.isNullOrEmpty())
    }

    @Test
    fun deleteKeyDeletesSelectedConnection() {
        val conn = ConnectionRef("source", "on", "target", "enabled")
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(
                    NodeRef(id = "source", type = "switch"),
                    NodeRef(id = "target", type = "display"),
                ),
                connections = listOf(conn),
            ),
        )

        state.dispatch(GraphynEditorIntent.SelectConnection(conn))
        state.dispatch(GraphynEditorIntent.DeleteSelectedConnection)

        assertNull(state.selectedConnection)
        assertEquals(emptyList(), state.workflow?.connections)
    }

    @Test
    fun reconnectSelectedConnectionReplacesTarget() {
        val original = ConnectionRef(
            fromNodeId = "source",
            fromPort = "on",
            toNodeId = "target-a",
            toPort = "enabled",
        )
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-reconnect",
                name = "Reconnect",
                nodes = listOf(
                    NodeRef(id = "source", type = "switch"),
                    NodeRef(id = "target-a", type = "display"),
                    NodeRef(id = "target-b", type = "display"),
                ),
                connections = listOf(original),
            ),
        )

        state.dispatch(GraphynEditorIntent.SelectConnection(original))
        state.dispatch(GraphynEditorIntent.ReconnectSelectedConnection("target-b", "enabled"))

        val expected = original.copy(toNodeId = "target-b", toPort = "enabled")
        assertEquals(listOf(expected), state.workflow?.connections)
        assertEquals(expected, state.selectedConnection)
    }

    @Test
    fun reconnectSelectedConnectionNoOpWhenNoneSelected() {
        val conn = ConnectionRef("source", "on", "target", "enabled")
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-noop",
                name = "Noop",
                nodes = listOf(
                    NodeRef(id = "source", type = "switch"),
                    NodeRef(id = "target", type = "display"),
                ),
                connections = listOf(conn),
            ),
        )

        state.dispatch(GraphynEditorIntent.ReconnectSelectedConnection("other", "enabled"))

        assertEquals(listOf(conn), state.workflow?.connections)
    }

    @Test
    fun showNodePickerStoresPickerState() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(NodeRef(id = "n1", type = "switch")),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.BeginConnection("n1", "on"))
        state.dispatch(GraphynEditorIntent.ShowNodePicker(
            screenPosition = androidx.compose.ui.geometry.Offset(100f, 200f),
            worldPosition = androidx.compose.ui.geometry.Offset(50f, 80f),
        ))

        assertNotNull(state.nodePickerState)
        assertEquals(androidx.compose.ui.geometry.Offset(100f, 200f), state.nodePickerState!!.screenPosition)
        assertEquals(androidx.compose.ui.geometry.Offset(50f, 80f), state.nodePickerState!!.worldPosition)
        assertNotNull(state.connectionDraft)
    }

    @Test
    fun dismissNodePickerClearsDraftAndPickerState() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(NodeRef(id = "n1", type = "switch")),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.BeginConnection("n1", "on"))
        state.dispatch(GraphynEditorIntent.ShowNodePicker(
            screenPosition = androidx.compose.ui.geometry.Offset(100f, 200f),
            worldPosition = androidx.compose.ui.geometry.Offset(50f, 80f),
        ))
        state.dispatch(GraphynEditorIntent.DismissNodePicker)

        assertNull(state.nodePickerState)
        assertNull(state.connectionDraft)
    }

    @Test
    fun addNodeAndConnectCreatesNodeAndConnection() {
        val switchSpec = NodeSpec(
            type = "switch",
            label = "Switch",
            inputs = listOf(PortSpec(name = "enabled", type = WorkflowType.BooleanType, required = false)),
            outputs = listOf(PortSpec(name = "on", type = WorkflowType.BooleanType)),
        )
        val displaySpec = NodeSpec(
            type = "display",
            label = "Display",
            inputs = listOf(PortSpec(name = "value", type = WorkflowType.BooleanType, required = false)),
            outputs = emptyList(),
        )
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "w",
                name = "W",
                nodes = listOf(NodeRef(id = "switch-1", type = "switch")),
                connections = emptyList(),
            ),
        )

        state.dispatch(GraphynEditorIntent.BeginConnection("switch-1", "on"))
        state.dispatch(GraphynEditorIntent.AddNodeAndConnect(
            spec = displaySpec,
            toPort = "value",
            worldPosition = androidx.compose.ui.geometry.Offset(400f, 100f),
        ))

        assertNull(state.connectionDraft)
        assertNull(state.nodePickerState)
        assertEquals(2, state.workflow?.nodes?.size)
        val conn = state.workflow?.connections?.firstOrNull()
        assertNotNull(conn)
        assertEquals("switch-1", conn!!.fromNodeId)
        assertEquals("on", conn.fromPort)
        assertEquals("value", conn.toPort)
    }

    @Test
    fun editorStateAppliesExecutionResults() {
        val specs = DefaultNodeSpecRegistry().apply {
            register(
                NodeSpec(
                    type = "switch",
                    label = "Switch",
                    inputs = listOf(
                        PortSpec(name = "enabled", type = WorkflowType.BooleanType, required = false),
                    ),
                    outputs = listOf(
                        PortSpec(name = "on", type = WorkflowType.BooleanType),
                    ),
                ),
            )
        }
        val executors = DefaultNodeExecutorRegistry().apply {
            register("switch") { input ->
                mapOf("on" to (input["enabled"] ?: WorkflowValue.BooleanValue(false)))
            }
        }
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-run",
                name = "Run",
                nodes = listOf(
                    NodeRef(id = "switch-1", type = "switch", config = mapOf("enabled" to WorkflowValue.BooleanValue(true))),
                ),
                connections = emptyList(),
            ),
        )
        val engine = WorkflowExecutionEngine(executors, specs)

        state.execute(engine)

        assertEquals(
            WorkflowValue.BooleanValue(true),
            state.outputsFor("switch-1")["on"],
        )
    }
}
