package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Applies [NodeRef.maxRetries] and [NodeRef.timeoutMs] around [block].
 *
 * A [TimeoutCancellationException] is converted to a [WorkflowExecutionException] so the
 * engine's resilience handler records it as a node error rather than a coroutine cancellation.
 */
internal suspend fun <T> runWithPolicy(
    node: NodeRef,
    block: suspend () -> T,
): T {
    val attempts = node.maxRetries.coerceAtLeast(0) + 1
    val timeoutMs = node.timeoutMs
    var lastError: Throwable = WorkflowExecutionException("No attempts made")
    repeat(attempts) {
        try {
            return if (timeoutMs != null) {
                try {
                    withTimeout(timeoutMs) { block() }
                } catch (e: TimeoutCancellationException) {
                    throw WorkflowExecutionException("Node '${node.id}' timed out after ${timeoutMs}ms")
                }
            } else {
                block()
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Throwable) { lastError = e }
    }
    throw lastError
}
