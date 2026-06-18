package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private fun intVal(n: Int) = WorkflowValue.IntValue(n)
private fun strVal(s: String) = WorkflowValue.StringValue(s)

private fun stateWith(vararg nodes: NodeRef): GraphynEditorState {
    val wf = WorkflowDefinition("wf", "W", nodes.toList(), emptyList())
    return GraphynEditorState(wf)
}

class NodeConfigStateTest {

    @Test
    fun updateNodeConfigPersistsValue() {
        val state = stateWith(NodeRef("a", "echo"))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "count", intVal(7)))
        assertEquals(intVal(7), state.workflow?.nodes?.first { it.id == "a" }?.config?.get("count"))
    }

    @Test
    fun multipleDispatchesAccumulateKeys() {
        val state = stateWith(NodeRef("a", "echo"))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "x", intVal(1)))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "y", intVal(2)))
        val cfg = state.workflow?.nodes?.first { it.id == "a" }?.config
        assertEquals(intVal(1), cfg?.get("x"))
        assertEquals(intVal(2), cfg?.get("y"))
    }

    @Test
    fun dispatchOverwritesExistingKey() {
        val state = stateWith(NodeRef("a", "echo", config = mapOf("count" to intVal(5))))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "count", intVal(10)))
        assertEquals(intVal(10), state.workflow?.nodes?.first { it.id == "a" }?.config?.get("count"))
    }

    @Test
    fun unknownNodeIdIsNoOp() {
        val state = stateWith(NodeRef("a", "echo", config = mapOf("count" to intVal(5))))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("missing", "count", intVal(99)))
        assertEquals(intVal(5), state.workflow?.nodes?.first { it.id == "a" }?.config?.get("count"))
    }

    @Test
    fun undoRevertsConfigChange() {
        val state = stateWith(NodeRef("a", "echo", config = mapOf("count" to intVal(5))))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "count", intVal(10)))
        state.dispatch(GraphynEditorIntent.Undo)
        assertEquals(intVal(5), state.workflow?.nodes?.first { it.id == "a" }?.config?.get("count"))
    }

    @Test
    fun redoReAppliesConfigChange() {
        val state = stateWith(NodeRef("a", "echo", config = mapOf("count" to intVal(5))))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "count", intVal(10)))
        state.dispatch(GraphynEditorIntent.Undo)
        state.dispatch(GraphynEditorIntent.Redo)
        assertEquals(intVal(10), state.workflow?.nodes?.first { it.id == "a" }?.config?.get("count"))
    }

    @Test
    fun configDoesNotAffectSiblingNode() {
        val state = stateWith(NodeRef("a", "echo"), NodeRef("b", "echo", config = mapOf("v" to intVal(3))))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "v", intVal(9)))
        assertEquals(intVal(3), state.workflow?.nodes?.first { it.id == "b" }?.config?.get("v"))
    }

    @Test
    fun configStringValueRoundTrips() {
        val state = stateWith(NodeRef("a", "echo"))
        state.dispatch(GraphynEditorIntent.UpdateNodeConfig("a", "mode", strVal("quality")))
        assertEquals(strVal("quality"), state.workflow?.nodes?.first { it.id == "a" }?.config?.get("mode"))
    }
}
