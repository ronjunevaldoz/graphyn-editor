package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

fun interface NodeExecutor {
    fun execute(input: Map<String, WorkflowValue>): Map<String, WorkflowValue>
}
