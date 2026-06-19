package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Executor for a single node type. The [execute] function is suspend so implementations can
 * perform async I/O (HTTP, file, database) without blocking the calling thread.
 *
 * Receives named input values and returns named output values. Both maps use port names as keys.
 */
fun interface NodeExecutor {
    suspend fun execute(input: Map<String, WorkflowValue>): Map<String, WorkflowValue>
}
