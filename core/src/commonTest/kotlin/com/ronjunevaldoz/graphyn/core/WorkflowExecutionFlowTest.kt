package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.ExecutionEvent
import com.ronjunevaldoz.graphyn.core.execution.ExecutionStreamMessage
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.executeAsFlow
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkflowExecutionFlowTest {

    private fun buildEngine(): WorkflowExecutionEngine {
        val reg = DefaultNodeExecutorRegistry()
        reg.register("flow.source") { _ -> mapOf("value" to WorkflowValue.IntValue(1)) }
        reg.register("flow.sink")   { inputs -> inputs }
        return WorkflowExecutionEngine(reg)
    }

    private val twoNodeWorkflow = WorkflowDefinition(
        id = "flow-test", name = "Flow Test",
        nodes = listOf(
            NodeRef("src",  "flow.source"),
            NodeRef("sink", "flow.sink"),
        ),
        connections = listOf(ConnectionRef("src", "value", "sink", "value")),
    )

    @Test
    fun flowEmitsPerNodeEventsThenCompleted() = runTest {
        val frames = buildEngine().executeAsFlow(twoNodeWorkflow).toList()

        val events = frames.filterIsInstance<ExecutionStreamMessage.Event>()
        assertTrue(events.isNotEmpty(), "Expected per-node event frames")
        assertTrue(events.any { it.event is ExecutionEvent.Succeeded }, "Expected at least one Succeeded event")

        val terminal = frames.last()
        assertIs<ExecutionStreamMessage.Completed>(terminal)
        assertTrue(terminal.result.isFullSuccess)
    }

    @Test
    fun flowEmitsFailedForStructuralError() = runTest {
        val cycleWorkflow = WorkflowDefinition(
            id = "cycle", name = "Cycle",
            nodes = listOf(NodeRef("a", "flow.source"), NodeRef("b", "flow.sink")),
            // Both connections form a cycle: a→b and b→a
            connections = listOf(
                ConnectionRef("a", "value", "b", "value"),
                ConnectionRef("b", "value", "a", "value"),
            ),
        )
        val frames = buildEngine().executeAsFlow(cycleWorkflow).toList()
        assertIs<ExecutionStreamMessage.Failed>(frames.last())
    }
}
