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

    /** Ordered, heterogeneous list. Element type is validated per the port's [WorkflowType.ListType]. */
    @Serializable
    @SerialName("list")
    data class ListValue(val items: List<WorkflowValue>) : WorkflowValue

    /** Named field map. Field types are validated per the port's [WorkflowType.RecordType]. */
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


fun Map<String, WorkflowValue>.stringOrError(key: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value
        ?.takeIf(String::isNotBlank)
        ?: error("Missing required string input '$key'.")

fun Map<String, WorkflowValue>.stringOr(key: String, default: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value ?: default
fun Map<String, WorkflowValue>.stringOrNull(key: String): String? =
    (this[key] as? WorkflowValue.StringValue)?.value

fun Map<String, WorkflowValue>.doubleOr(key: String, default: Double): Double =
    when (val value = this[key]) {
        is WorkflowValue.DoubleValue -> value.value
        is WorkflowValue.IntValue -> value.value.toDouble()
        else -> default
    }

fun Map<String, WorkflowValue>.intOr(key: String, default: Int): Int =
    (this[key] as? WorkflowValue.IntValue)?.value ?: default


fun Map<String, WorkflowValue>.listOrError(key: String): List<WorkflowValue> =
    (this[key] as? WorkflowValue.ListValue)?.items ?: error("Missing required list input '$key'.")

fun Map<String, WorkflowValue>.listOrEmpty(key: String): List<WorkflowValue> =
    (this[key] as? WorkflowValue.ListValue)?.items.orEmpty()

fun Map<String, WorkflowValue>.recordOrError(key: String): Map<String, WorkflowValue> =
    (this[key] as? WorkflowValue.RecordValue)?.fields
        ?: error("Missing required record input '$key'.")

fun WorkflowValue.numberOrError(): Double = when (this) {
    is WorkflowValue.DoubleValue -> value
    is WorkflowValue.IntValue -> value.toDouble()
    else -> error("Expected a numeric value.")
}

fun Map<String, WorkflowValue>.numberOr(key: String, default: Double): Double =
    this[key]?.takeUnless { it == WorkflowValue.NullValue }?.numberOrError() ?: default

fun Map<String, WorkflowValue>.intOrError(key: String): Int =
    (this[key] as? WorkflowValue.IntValue)?.value ?: error("Expected integer field '$key'.")

fun Map<String, WorkflowValue>.booleanOr(name: String, default: Boolean): Boolean {
    return (getValue(name) as? WorkflowValue.BooleanValue)?.value ?: default
}

fun Map<String, WorkflowValue>.booleanOrError(name: String): Boolean {
    return (getValue(name) as? WorkflowValue.BooleanValue)?.value
        ?: error("Expected boolean field '$name'.")
}
fun WorkflowValue.record(): Map<String, WorkflowValue> =
    (this as? WorkflowValue.RecordValue)?.fields ?: error("Expected a record value.")