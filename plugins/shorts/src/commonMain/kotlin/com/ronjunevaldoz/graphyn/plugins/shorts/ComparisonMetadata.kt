package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.time.Clock

/**
 * Adds a generation timestamp to the comparison result before it is serialized to disk.
 *
 * The comparison payload itself stays intact; we only enrich the top-level record with
 * `generated_at` so the saved artifact tells us when it was produced.
 */
internal val comparisonMetadataSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_METADATA,
    label = "Comparison Metadata",
    description = "Appends a generation timestamp to the validated comparison record.",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Comparison record with generated_at")),
)

/** Executor for [comparisonMetadataSpec]. */
internal val comparisonMetadataExecutor: NodeExecutor = NodeExecutor { inputs ->
    val generatedAt = WorkflowValue.StringValue(Clock.System.now().toString())
    val input = inputs["input"]
    val value = if (input is WorkflowValue.RecordValue) {
        WorkflowValue.RecordValue(input.fields + ("generated_at" to generatedAt))
    } else {
        WorkflowValue.RecordValue(mapOf("generated_at" to generatedAt, "value" to (input ?: WorkflowValue.NullValue)))
    }
    mapOf("value" to value)
}
