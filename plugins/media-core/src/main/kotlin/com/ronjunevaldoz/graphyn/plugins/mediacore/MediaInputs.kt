package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Shared, typed accessors for reading node executor inputs across the media-core executors. */
internal fun Map<String, WorkflowValue>.string(key: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value
        ?.takeIf(String::isNotBlank)
        ?: error("Missing required string input '$key'.")

internal fun Map<String, WorkflowValue>.stringOr(key: String, default: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value ?: default

internal fun Map<String, WorkflowValue>.list(key: String): List<WorkflowValue> =
    (this[key] as? WorkflowValue.ListValue)?.items ?: error("Missing required list input '$key'.")

internal fun Map<String, WorkflowValue>.listOrEmpty(key: String): List<WorkflowValue> =
    (this[key] as? WorkflowValue.ListValue)?.items.orEmpty()

internal fun Map<String, WorkflowValue>.record(key: String): Map<String, WorkflowValue> =
    (this[key] as? WorkflowValue.RecordValue)?.fields ?: error("Missing required record input '$key'.")

internal fun WorkflowValue.number(): Double = when (this) {
    is WorkflowValue.DoubleValue -> value
    is WorkflowValue.IntValue -> value.toDouble()
    else -> error("Expected a numeric value.")
}

internal fun Map<String, WorkflowValue>.numberOr(key: String, default: Double): Double =
    this[key]?.takeUnless { it == WorkflowValue.NullValue }?.number() ?: default

internal fun Map<String, WorkflowValue>.intField(key: String): Int =
    (this[key] as? WorkflowValue.IntValue)?.value ?: error("Expected integer field '$key'.")
