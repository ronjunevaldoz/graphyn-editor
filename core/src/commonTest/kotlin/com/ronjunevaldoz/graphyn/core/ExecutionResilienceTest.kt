package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.ExecutionEvent
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExecutionResilienceTest {

    private fun engine(): WorkflowExecutionEngine {
        val executors = DefaultNodeExecutorRegistry()
        executors.register("ok") { mapOf("out" to WorkflowValue.IntValue(1)) }
        executors.register("boom") { throw IllegalStateException("kaboom") }
        return WorkflowExecutionEngine(executors)
    }

    // a(ok) -> b(boom) -> c(ok) ;  d(ok) is independent
    private val workflow = WorkflowDefinition(
        id = "resilience", name = "Resilience",
        nodes = listOf(
            NodeRef("a", "ok"), NodeRef("b", "boom"), NodeRef("c", "ok"), NodeRef("d", "ok"),
        ),
        connections = listOf(
            ConnectionRef("a", "out", "b", "in"),
            ConnectionRef("b", "out", "c", "in"),
        ),
    )

    @Test
    fun failingNodeIsIsolatedAndDependentsAreSkipped() = runTest {
        val result = engine().execute(workflow)

        assertEquals(NodeExecutionStatus.Success, result.statusByNodeId["a"])
        assertEquals(NodeExecutionStatus.Error,   result.statusByNodeId["b"])
        assertEquals(NodeExecutionStatus.Skipped, result.statusByNodeId["c"], "dependent of a failed node must be skipped")
        assertEquals(NodeExecutionStatus.Success, result.statusByNodeId["d"], "independent branch must still run")

        assertEquals("kaboom", result.errorsByNodeId["b"])
        assertTrue("c" !in result.nodeOutputsByNodeId, "skipped node produces no output")
        assertEquals(2, result.successCount)
        assertEquals(1, result.errorCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun emitsLiveEventsPerNode() = runTest {
        val events = mutableListOf<ExecutionEvent>()
        engine().execute(workflow) { events += it }

        assertTrue(events.any { it is ExecutionEvent.Started && it.nodeId == "a" })
        assertTrue(events.any { it is ExecutionEvent.Failed && it.nodeId == "b" })
        assertTrue(events.any { it is ExecutionEvent.Skipped && it.nodeId == "c" && it.causeNodeId == "b" })
        assertTrue(events.any { it is ExecutionEvent.Succeeded && it.nodeId == "d" })
    }
}
