package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Streaming variant of [WorkflowExecutionEngine.execute] as a cold [Flow].
 *
 * Emits [ExecutionStreamMessage.Event] for each node progress event, followed by exactly one
 * terminal frame: [ExecutionStreamMessage.Completed] on success, or
 * [ExecutionStreamMessage.Failed] if a structural error (cycle, duplicate ids) prevents execution.
 *
 * ```kotlin
 * engine.executeAsFlow(workflow).collect { msg ->
 *     when (msg) {
 *         is ExecutionStreamMessage.Event     -> updateStatus(msg.event)
 *         is ExecutionStreamMessage.Completed -> showResult(msg.result)
 *         is ExecutionStreamMessage.Failed    -> showError(msg.message)
 *     }
 * }
 * ```
 */
fun WorkflowExecutionEngine.executeAsFlow(
    workflow: WorkflowDefinition,
): Flow<ExecutionStreamMessage> = channelFlow {
    try {
        val result = execute(workflow, onEvent = { event -> trySend(ExecutionStreamMessage.Event(event)) })
        send(ExecutionStreamMessage.Completed(result))
    } catch (e: Exception) {
        send(ExecutionStreamMessage.Failed(e.message ?: e::class.simpleName ?: "Unknown error"))
    }
}
