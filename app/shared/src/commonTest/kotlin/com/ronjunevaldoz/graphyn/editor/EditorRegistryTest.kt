package com.ronjunevaldoz.graphyn.editor

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.DefaultGraphynEditorPluginContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditorRegistryTest {
    @Test
    fun editorPanelsCanBeRegisteredAndResolved() {
        val registry = DefaultEditorPanelRegistry()
        val context = DefaultGraphynEditorPluginContext(registry)

        context.registerPanel(
            "printer",
            EditorPanelFactory { _: EditorPanelContext -> }
        )

        assertNotNull(registry.resolve("printer"))
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
}
