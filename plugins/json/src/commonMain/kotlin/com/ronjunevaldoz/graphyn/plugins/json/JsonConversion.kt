package com.ronjunevaldoz.graphyn.plugins.json

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Maps a parsed [JsonElement] onto the workflow's [WorkflowValue] tree. */
internal fun JsonElement.toWorkflowValue(): WorkflowValue = when (this) {
    is JsonNull -> WorkflowValue.NullValue
    is JsonObject -> WorkflowValue.RecordValue(mapValues { (_, v) -> v.toWorkflowValue() })
    is JsonArray -> WorkflowValue.ListValue(map { it.toWorkflowValue() })
    is JsonPrimitive -> when {
        isString -> WorkflowValue.StringValue(content)
        booleanOrNull != null -> WorkflowValue.BooleanValue(booleanOrNull!!)
        intOrNull != null -> WorkflowValue.IntValue(intOrNull!!)
        doubleOrNull != null -> WorkflowValue.DoubleValue(doubleOrNull!!)
        else -> WorkflowValue.StringValue(content)
    }
}

/** Maps a [WorkflowValue] back onto a serializable [JsonElement]. */
internal fun WorkflowValue.toJsonElement(): JsonElement = when (this) {
    is WorkflowValue.StringValue -> JsonPrimitive(value)
    is WorkflowValue.IntValue -> JsonPrimitive(value)
    is WorkflowValue.DoubleValue -> JsonPrimitive(value)
    is WorkflowValue.BooleanValue -> JsonPrimitive(value)
    is WorkflowValue.ListValue -> buildJsonArray { items.forEach { add(it.toJsonElement()) } }
    is WorkflowValue.RecordValue -> buildJsonObject { fields.forEach { (k, v) -> put(k, v.toJsonElement()) } }
    WorkflowValue.NullValue -> JsonNull
    WorkflowValue.OpaqueValue -> JsonNull
}

/**
 * Resolves a dotted path (e.g. `items.0.name`) against a [WorkflowValue] tree.
 * Record fields are addressed by key; list elements by integer index.
 * Returns null if any segment is missing.
 */
internal fun WorkflowValue.resolvePath(path: String): WorkflowValue? {
    if (path.isBlank()) return this
    var current: WorkflowValue = this
    for (segment in path.split('.')) {
        current = when (current) {
            is WorkflowValue.RecordValue -> current.fields[segment] ?: return null
            is WorkflowValue.ListValue -> segment.toIntOrNull()
                ?.let { current.items.getOrNull(it) } ?: return null
            else -> return null
        }
    }
    return current
}
