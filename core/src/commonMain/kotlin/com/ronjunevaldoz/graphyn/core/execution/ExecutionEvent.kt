package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Emitted by [WorkflowExecutionEngine] as each node progresses, so a host can reflect
 * live status on the canvas instead of waiting for the whole run to finish.
 *
 * Handlers run on the engine's calling coroutine; keep them cheap and non-blocking.
 * Updating thread-safe Compose snapshot state directly is fine.
 */
sealed interface ExecutionEvent {
    val nodeId: String

    /** The node is about to execute. */
    data class Started(override val nodeId: String) : ExecutionEvent

    /** The node finished successfully with [outputs] in [durationMs]. */
    data class Succeeded(
        override val nodeId: String,
        val outputs: Map<String, WorkflowValue>,
        val durationMs: Long,
    ) : ExecutionEvent

    /** The node threw; [message] is the failure reason and [durationMs] the time spent before it failed. */
    data class Failed(
        override val nodeId: String,
        val message: String,
        val durationMs: Long,
    ) : ExecutionEvent

    /** The node was not run because [causeNodeId] (an upstream dependency) failed. */
    data class Skipped(
        override val nodeId: String,
        val causeNodeId: String,
    ) : ExecutionEvent
}
