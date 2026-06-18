package com.ronjunevaldoz.graphyn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A concrete value flowing through the workflow graph at runtime.
 *
 * Values are produced by node executors and consumed by downstream nodes.
 * The type of a value must be compatible with the [WorkflowType] of the
 * port it connects to, as determined by [WorkflowTypeCompatibility].
 *
 * [OpaqueValue] is a sentinel for runtime handles (textures, file handles, etc.)
 * that the workflow model should not inspect or serialize meaningfully.
 */
@Serializable
sealed interface WorkflowValue {
    @Serializable @SerialName("string")  data class StringValue(val value: String)   : WorkflowValue
    @Serializable @SerialName("int")     data class IntValue(val value: Int)          : WorkflowValue
    @Serializable @SerialName("double")  data class DoubleValue(val value: Double)    : WorkflowValue
    @Serializable @SerialName("boolean") data class BooleanValue(val value: Boolean)  : WorkflowValue

    /** Ordered, heterogeneous list. Element type is validated per the port's [WorkflowType.ListType]. */
    @Serializable @SerialName("list")    data class ListValue(val items: List<WorkflowValue>) : WorkflowValue

    /** Named field map. Field types are validated per the port's [WorkflowType.RecordType]. */
    @Serializable @SerialName("record")  data class RecordValue(val fields: Map<String, WorkflowValue>) : WorkflowValue

    @Serializable @SerialName("null")    data object NullValue   : WorkflowValue
    @Serializable @SerialName("opaque")  data object OpaqueValue : WorkflowValue
}
