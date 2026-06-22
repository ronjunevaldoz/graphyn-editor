package com.ronjunevaldoz.graphyn.core.execution

import kotlinx.serialization.Serializable

/**
 * One frame in a streamed workflow run (e.g. server-sent events).
 *
 * A stream emits zero or more [Event] frames as nodes progress, then exactly one terminal
 * frame — [Completed] with the final [WorkflowExecutionResult], or [Failed] if the run could
 * not produce a result at all. Consumers should stop reading after the first terminal frame.
 *
 * Both ends share this type via `:core`, so a remote stream stays as type-safe as an in-process
 * [ExecutionEvent] callback — only the transport is JSON.
 */
@Serializable
sealed interface ExecutionStreamMessage {
    /** A per-node progress event. */
    @Serializable
    data class Event(val event: ExecutionEvent) : ExecutionStreamMessage

    /** Terminal: the run finished (possibly with per-node errors — inspect [result]). */
    @Serializable
    data class Completed(val result: WorkflowExecutionResult) : ExecutionStreamMessage

    /** Terminal: the run could not complete (e.g. a cycle or duplicate ids rejected before any node ran). */
    @Serializable
    data class Failed(val message: String) : ExecutionStreamMessage
}
