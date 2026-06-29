package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.ListValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.NullValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue

/** Typed accessors for reading [WorkflowValue] from argument maps. */
internal fun Map<String, WorkflowValue>.str(key: String): String? =
    when (val v = get(key)) { is StringValue -> v.value.takeIf { it.isNotBlank() }; else -> null }

internal fun Map<String, WorkflowValue>.int(key: String): Int? =
    when (val v = get(key)) { is IntValue -> v.value; else -> null }

internal fun Map<String, WorkflowValue>.double(key: String): Double? =
    when (val v = get(key)) { is DoubleValue -> v.value; is IntValue -> v.value.toDouble(); else -> null }

internal fun Map<String, WorkflowValue>.bool(key: String): Boolean? =
    when (val v = get(key)) { is BooleanValue -> v.value; else -> null }

internal fun WorkflowValue.asList(): List<WorkflowValue>? =
    when (this) { is ListValue -> items.takeIf { it.isNotEmpty() }; is NullValue -> null; else -> null }
