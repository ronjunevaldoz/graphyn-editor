package com.ronjunevaldoz.graphyn.core.model

object WorkflowValueFlattener {
    fun flattenMap(values: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        return values.flatMap { (key, value) -> flatten(value, key).entries }
            .associate { it.key to it.value }
    }

    fun flatten(value: WorkflowValue, prefix: String): Map<String, WorkflowValue> = when (value) {
        is WorkflowValue.StringValue,
        is WorkflowValue.IntValue,
        is WorkflowValue.DoubleValue,
        is WorkflowValue.BooleanValue,
        WorkflowValue.NullValue,
        WorkflowValue.OpaqueValue,
        -> mapOf(prefix to value)

        is WorkflowValue.ListValue -> value.items.flatMapIndexed { index, item ->
            flatten(item, "$prefix[$index]").entries
        }.associate { it.key to it.value }

        is WorkflowValue.RecordValue -> value.fields.flatMap { (key, item) ->
            val childPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
            flatten(item, childPrefix).entries
        }.associate { it.key to it.value }
    }
}
