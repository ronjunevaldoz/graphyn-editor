package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.ExecutionStreamMessage
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks in-flight and recently-finished workflow runs so they can be observed as event streams.
 *
 * [start] kicks off execution immediately and returns a run id; subscribers to [messages] receive
 * the full history (the per-run flow replays all frames) followed by live frames until a terminal
 * [ExecutionStreamMessage.Completed]/[ExecutionStreamMessage.Failed]. Storage is in-memory and not
 * evicted — adequate for a single-node deployment; a durable store arrives with persistence (M2).
 */
class GraphynRunRegistry(
    private val engine: WorkflowExecutionEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    /** Maximum number of workflows that may execute concurrently. Excess requests return null. */
    val maxConcurrentRuns: Int = 10,
) {
    private val runs = ConcurrentHashMap<String, MutableSharedFlow<ExecutionStreamMessage>>()
    private val activeCount = AtomicInteger(0)

    /** Returns false when the concurrent-run limit is reached. */
    val canAcceptRun: Boolean get() = activeCount.get() < maxConcurrentRuns

    /**
     * Validates nothing here — callers should validate first and check [canAcceptRun].
     * Starts the run and returns its id.
     */
    fun start(workflow: WorkflowDefinition): String {
        val runId = UUID.randomUUID().toString()
        val flow = MutableSharedFlow<ExecutionStreamMessage>(replay = Int.MAX_VALUE)
        runs[runId] = flow
        activeCount.incrementAndGet()
        scope.launch {
            val terminal = try {
                val result = engine.execute(workflow) { event ->
                    flow.tryEmit(ExecutionStreamMessage.Event(event))
                }
                ExecutionStreamMessage.Completed(result)
            } catch (e: Throwable) {
                ExecutionStreamMessage.Failed(e.message ?: "Execution failed")
            } finally {
                activeCount.decrementAndGet()
            }
            flow.emit(terminal)
        }
        return runId
    }

    /** The frame stream for [runId], or null if no such run exists. */
    fun messages(runId: String): SharedFlow<ExecutionStreamMessage>? = runs[runId]?.asSharedFlow()
}
