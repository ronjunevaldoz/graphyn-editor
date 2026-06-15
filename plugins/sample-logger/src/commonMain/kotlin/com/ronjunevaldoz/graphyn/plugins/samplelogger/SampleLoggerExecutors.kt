package com.ronjunevaldoz.graphyn.plugins.samplelogger

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

object SampleLoggerExecutors {
    fun log(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val message = input["message"] as? WorkflowValue.StringValue ?: WorkflowValue.StringValue("")
        return mapOf("message" to message)
    }
}
