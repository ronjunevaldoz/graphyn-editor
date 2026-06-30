package com.ronjunevaldoz.graphyn.plugins.preview

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Platform-specific rendering of the preview card's content area. */
@Composable
internal expect fun PreviewContentArea(value: WorkflowValue?)

internal fun WorkflowValue?.display(): String = when (this) {
    null, is WorkflowValue.NullValue -> "No output yet"
    is WorkflowValue.StringValue     -> value.take(120)
    is WorkflowValue.IntValue        -> value.toString()
    is WorkflowValue.DoubleValue     -> value.toString()
    is WorkflowValue.BooleanValue    -> if (value) "true" else "false"
    is WorkflowValue.ListValue       -> "[ ${items.size} item${if (items.size == 1) "" else "s"} ]"
    is WorkflowValue.RecordValue     -> "{ ${fields.keys.take(3).joinToString(", ")}${if (fields.size > 3) "…" else ""} }"
    else                             -> toString()
}
