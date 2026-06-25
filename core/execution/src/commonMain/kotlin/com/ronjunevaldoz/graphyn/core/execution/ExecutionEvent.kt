package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.serialization.Serializable

/**
 * Emitted by [WorkflowExecutionEngine] as each node progresses, so a host can reflect
 * live status on the canvas instead of waiting for the whole run to finish.
 *
 * Handlers run on the engine's calling coroutine; keep them cheap and non-blocking.
 * Updating thread-safe Compose snapshot state directly is fine.
 *
 * Serializable so it can be streamed to remote hosts (see [ExecutionStreamMessage]).
 */
@Serializable
sealed interface ExecutionEvent {
    val nodeId: String

    /** The node is about to execute. */
    @Serializable
    data class Started(override val nodeId: String) : ExecutionEvent

    /** The node finished successfully with [outputs] in [durationMs]. */
    @Serializable
    data class Succeeded(
        override val nodeId: String,
        val outputs: Map<String, WorkflowValue>,
        val durationMs: Long,
    ) : ExecutionEvent

    /** The node threw; [message] is the failure reason and [durationMs] the time spent before it failed. */
    @Serializable
    data class Failed(
        override val nodeId: String,
        val message: String,
        val durationMs: Long,
    ) : ExecutionEvent

    /** The node was not run because [causeNodeId] (an upstream dependency) failed. */
    @Serializable
    data class Skipped(
        override val nodeId: String,
        val causeNodeId: String,
    ) : ExecutionEvent
}
