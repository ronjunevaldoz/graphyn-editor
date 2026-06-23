@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.GRAPHYN_SUBGRAPH_TYPE
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SubgraphCollapseDispatchTest {

    private val specs = DefaultNodeSpecRegistry().apply {
        register(NodeSpec("op", "Op",
            inputs = listOf(PortSpec("in", WorkflowType.StringType)),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))))
    }

    private fun chainState() = GraphynEditorState(
        WorkflowDefinition(
            id = "wf", name = "Chain",
            nodes = listOf(NodeRef("a", "op"), NodeRef("b", "op"), NodeRef("c", "op"), NodeRef("d", "op")),
            connections = listOf(
                ConnectionRef("a", "out", "b", "in"),
                ConnectionRef("b", "out", "c", "in"),
                ConnectionRef("c", "out", "d", "in"),
            ),
        ),
        nodeSpecs = specs,
    )

    @Test
    fun collapseIntentReplacesSelectionWithSubgraphNode() {
        val state = chainState()
        state.selectedNodeIds = setOf("b", "c")

        state.dispatch(GraphynEditorIntent.CollapseSelectionToSubgraph)

        val wf = state.workflow!!
        assertTrue(wf.nodes.none { it.id == "b" || it.id == "c" })
        val sg = wf.nodes.first { it.type == GRAPHYN_SUBGRAPH_TYPE }
        assertEquals(2, sg.subgraph?.nodes?.size)
        assertEquals(sg.id, state.selectedNodeId)
    }

    @Test
    fun collapseThenExpandRestoresOriginalNodes() {
        val state = chainState()
        state.selectedNodeIds = setOf("b", "c")
        state.dispatch(GraphynEditorIntent.CollapseSelectionToSubgraph)
        val sgId = state.workflow!!.nodes.first { it.type == GRAPHYN_SUBGRAPH_TYPE }.id

        state.dispatch(GraphynEditorIntent.ExpandSubgraph(sgId))

        val wf = state.workflow!!
        assertEquals(setOf("a", "b", "c", "d"), wf.nodes.mapTo(mutableSetOf()) { it.id })
        assertTrue(wf.connections.any { it.fromNodeId == "a" && it.toNodeId == "b" })
        assertTrue(wf.connections.any { it.fromNodeId == "c" && it.toNodeId == "d" })
    }

    @Test
    fun collapseIsUndoable() {
        val state = chainState()
        state.selectedNodeIds = setOf("b", "c")
        state.dispatch(GraphynEditorIntent.CollapseSelectionToSubgraph)
        assertNotNull(state.workflow!!.nodes.firstOrNull { it.type == GRAPHYN_SUBGRAPH_TYPE })

        state.dispatch(GraphynEditorIntent.Undo)

        assertEquals(setOf("a", "b", "c", "d"), state.workflow!!.nodes.mapTo(mutableSetOf()) { it.id })
    }
}
