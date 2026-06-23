package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.ExecutionEvent
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun nodeTimeoutIsRecordedAsError() = runTest {
        val executors = DefaultNodeExecutorRegistry()
        executors.register("slow") { delay(10_000); mapOf("out" to WorkflowValue.IntValue(1)) }
        val eng = WorkflowExecutionEngine(executors)
        val wf = WorkflowDefinition(
            id = "timeout-test", name = "Timeout",
            nodes = listOf(NodeRef("s", "slow", timeoutMs = 50)),
            connections = emptyList(),
        )
        val result = eng.execute(wf)
        assertEquals(NodeExecutionStatus.Error, result.statusByNodeId["s"])
        assertNotNull(result.errorsByNodeId["s"])
        assertTrue(result.errorsByNodeId["s"]!!.contains("timed out"), "error message should mention timeout")
    }

    @Test
    fun nodeRetriesOnFailureThenSucceeds() = runTest {
        var attempts = 0
        val executors = DefaultNodeExecutorRegistry()
        executors.register("flaky") {
            attempts++
            if (attempts < 3) throw IllegalStateException("not yet")
            mapOf("out" to WorkflowValue.IntValue(attempts))
        }
        val eng = WorkflowExecutionEngine(executors)
        val wf = WorkflowDefinition(
            id = "retry-test", name = "Retry",
            nodes = listOf(NodeRef("f", "flaky", maxRetries = 2)),
            connections = emptyList(),
        )
        val result = eng.execute(wf)
        assertEquals(NodeExecutionStatus.Success, result.statusByNodeId["f"])
        assertEquals(3, attempts)
    }

    @Test
    fun independentNodesRunInParallelLayer() = runTest {
        val executors = DefaultNodeExecutorRegistry()
        executors.register("a") { mapOf("out" to WorkflowValue.IntValue(1)) }
        executors.register("b") { mapOf("out" to WorkflowValue.IntValue(2)) }
        executors.register("merge") { inputs ->
            val sum = (inputs["x"] as? WorkflowValue.IntValue)!!.value +
                (inputs["y"] as? WorkflowValue.IntValue)!!.value
            mapOf("out" to WorkflowValue.IntValue(sum))
        }
        val eng = WorkflowExecutionEngine(executors)
        val wf = WorkflowDefinition(
            id = "parallel", name = "Parallel",
            nodes = listOf(NodeRef("a", "a"), NodeRef("b", "b"), NodeRef("m", "merge")),
            connections = listOf(
                ConnectionRef("a", "out", "m", "x"),
                ConnectionRef("b", "out", "m", "y"),
            ),
        )
        val result = eng.execute(wf)
        assertTrue(result.isFullSuccess)
        assertEquals(WorkflowValue.IntValue(3), result.nodeOutputsByNodeId["m"]?.get("out"))
    }

    @Test
    fun emitsLiveEventsPerNode() = runTest {
        val events = mutableListOf<ExecutionEvent>()
        engine().execute(workflow, onEvent = { events += it })

        assertTrue(events.any { it is ExecutionEvent.Started && it.nodeId == "a" })
        assertTrue(events.any { it is ExecutionEvent.Failed && it.nodeId == "b" })
        assertTrue(events.any { it is ExecutionEvent.Skipped && it.nodeId == "c" && it.causeNodeId == "b" })
        assertTrue(events.any { it is ExecutionEvent.Succeeded && it.nodeId == "d" })
    }
}
