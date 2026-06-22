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
) {
    private val runs = ConcurrentHashMap<String, MutableSharedFlow<ExecutionStreamMessage>>()

    /** Validates nothing here — callers should validate first. Starts the run and returns its id. */
    fun start(workflow: WorkflowDefinition): String {
        val runId = UUID.randomUUID().toString()
        // replay = unlimited so a late subscriber still receives every frame from the start.
        val flow = MutableSharedFlow<ExecutionStreamMessage>(replay = Int.MAX_VALUE)
        runs[runId] = flow
        scope.launch {
            val terminal = try {
                val result = engine.execute(workflow) { event ->
                    flow.tryEmit(ExecutionStreamMessage.Event(event))
                }
                ExecutionStreamMessage.Completed(result)
            } catch (e: Throwable) {
                ExecutionStreamMessage.Failed(e.message ?: "Execution failed")
            }
            flow.emit(terminal)
        }
        return runId
    }

    /** The frame stream for [runId], or null if no such run exists. */
    fun messages(runId: String): SharedFlow<ExecutionStreamMessage>? = runs[runId]?.asSharedFlow()
}
