package com.ronjunevaldoz.graphyn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkflowValue {
    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : WorkflowValue

    @Serializable
    @SerialName("int")
    data class IntValue(val value: Int) : WorkflowValue

    @Serializable
    @SerialName("double")
    data class DoubleValue(val value: Double) : WorkflowValue

    @Serializable
    @SerialName("boolean")
    data class BooleanValue(val value: Boolean) : WorkflowValue

    @Serializable
    @SerialName("list")
    data class ListValue(val items: List<WorkflowValue>) : WorkflowValue

    @Serializable
    @SerialName("record")
    data class RecordValue(val fields: Map<String, WorkflowValue>) : WorkflowValue

    @Serializable
    @SerialName("null")
    data object NullValue : WorkflowValue

    @Serializable
    @SerialName("opaque")
    data object OpaqueValue : WorkflowValue
}
