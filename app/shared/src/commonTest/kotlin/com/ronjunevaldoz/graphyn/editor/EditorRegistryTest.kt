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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        state.moveNode("first", IntOffset(10, 20))

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
