package com.ronjunevaldoz.graphyn.core.model

sealed interface WorkflowValue {
    data class StringValue(val value: String) : WorkflowValue
    data class IntValue(val value: Int) : WorkflowValue
    data class DoubleValue(val value: Double) : WorkflowValue
    data class BooleanValue(val value: Boolean) : WorkflowValue
    data class ListValue(val items: List<WorkflowValue>) : WorkflowValue
    data class RecordValue(val fields: Map<String, WorkflowValue>) : WorkflowValue
    data object NullValue : WorkflowValue
    data object OpaqueValue : WorkflowValue
}
