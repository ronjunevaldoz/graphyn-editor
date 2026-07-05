package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.ExecutionEvent
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.reportProgress
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressReportingTest {

    @Test
    fun executorReportsProgressInterleavedBetweenStartedAndSucceeded() = runTest {
        val executors = DefaultNodeExecutorRegistry()
        executors.register("diffuse") {
            repeat(3) { i -> reportProgress(step = i + 1, total = 3, phase = "denoise") }
            mapOf("image" to WorkflowValue.StringValue("out.png"))
        }
        val eng = WorkflowExecutionEngine(executors)
        val wf = WorkflowDefinition(
            id = "progress", name = "Progress",
            nodes = listOf(NodeRef("n", "diffuse")),
            connections = emptyList(),
        )

        val events = mutableListOf<ExecutionEvent>()
        eng.execute(wf, onEvent = { events += it })

        val progress = events.filterIsInstance<ExecutionEvent.Progress>()
        assertEquals(3, progress.size, "every reportProgress call must surface as a Progress event")
        assertEquals(listOf(1, 2, 3), progress.map { it.step })
        assertTrue(progress.all { it.total == 3 && it.phase == "denoise" && it.nodeId == "n" })

        val startedIdx = events.indexOfFirst { it is ExecutionEvent.Started }
        val succeededIdx = events.indexOfFirst { it is ExecutionEvent.Succeeded }
        val firstProgressIdx = events.indexOfFirst { it is ExecutionEvent.Progress }
        val lastProgressIdx = events.indexOfLast { it is ExecutionEvent.Progress }
        assertTrue(startedIdx < firstProgressIdx, "Progress must come after Started")
        assertTrue(lastProgressIdx < succeededIdx, "Progress must come before Succeeded")
    }

    @Test
    fun executorsThatNeverReportProduceNoProgressEvents() = runTest {
        val executors = DefaultNodeExecutorRegistry()
        executors.register("plain") { mapOf("out" to WorkflowValue.IntValue(1)) }
        val eng = WorkflowExecutionEngine(executors)
        val wf = WorkflowDefinition(
            id = "no-progress", name = "NoProgress",
            nodes = listOf(NodeRef("p", "plain")),
            connections = emptyList(),
        )

        val events = mutableListOf<ExecutionEvent>()
        eng.execute(wf, onEvent = { events += it })

        assertTrue(events.none { it is ExecutionEvent.Progress })
    }
}
