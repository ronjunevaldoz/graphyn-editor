package com.ronjunevaldoz.graphyn.core.execution

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Opt-in channel for a long-running [NodeExecutor] to report intermediate progress mid-execution
 * (e.g. an image node emitting each of its 20 diffusion steps) without changing the [NodeExecutor]
 * signature. The engine installs an instance into the coroutine context before invoking each node's
 * `execute`, so an executor reports simply by calling [reportProgress] — existing executors that
 * never call it are entirely unaffected.
 *
 * A report surfaces as an [ExecutionEvent.Progress] frame interleaved between the node's
 * [ExecutionEvent.Started] and its terminal [ExecutionEvent.Succeeded]/[ExecutionEvent.Failed],
 * so remote SSE consumers see step-level progress through the same stream as node lifecycle.
 *
 * ```kotlin
 * NodeExecutor { inputs ->
 *     repeat(totalSteps) { i -> reportProgress(step = i + 1, total = totalSteps, phase = "denoise") }
 *     mapOf("image" to WorkflowValue.StringValue(path))
 * }
 * ```
 */
interface ProgressReporter : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Key

    /**
     * Reports that the current node is at [step] of [total], optionally in a named [phase].
     * Called from inside an executor body. Implementations must be cheap and non-blocking.
     */
    fun report(step: Int, total: Int, phase: String? = null)

    companion object Key : CoroutineContext.Key<ProgressReporter>
}

/**
 * Reports intra-node progress from inside a [NodeExecutor] body. A no-op if no reporter is
 * installed (e.g. the node runs outside [WorkflowExecutionEngine]), so it is always safe to call.
 */
suspend fun reportProgress(step: Int, total: Int, phase: String? = null) {
    coroutineContext[ProgressReporter]?.report(step, total, phase)
}
