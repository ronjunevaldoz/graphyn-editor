package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr

// Compiled executor for the comparison-arc's Ollama request. Sibling to [ollamaBodySpec] in
// StoryboardRequestNodes.kt — same script.eval-avoidance reasoning documented there — reuses
// [ollamaUrlSpec]/[ollamaUrlExecutor] as-is since the URL-building logic doesn't depend on the
// prompt shape.

/** Builds the Ollama `/api/generate` request body for the comparison-arc prompt. */
public val comparisonOllamaBodySpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_OLLAMA_BODY, label = "Comparison Ollama Body", category = ShortsConstants.CATEGORY,
    description = "Builds the Ollama /api/generate request body for the comparison-arc prompt.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false), PortSpec("topic", WorkflowType.StringType)),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType)),
)

/** Executor for [comparisonOllamaBodySpec]. */
public val comparisonOllamaBodyExecutor: NodeExecutor = NodeExecutor { inputs ->
    val model = (inputs["input"] as? WorkflowValue.StringValue)?.value?.ifBlank { null } ?: "llama3.1"
    val topic = inputs.stringOr("topic", "")
    mapOf("result" to WorkflowValue.RecordValue(
        mapOf(
            "model" to WorkflowValue.StringValue(model),
            "prompt" to WorkflowValue.StringValue(buildComparisonPrompt(topic)),
            "stream" to WorkflowValue.BooleanValue(false),
            "format" to WorkflowValue.StringValue("json"),
            "keep_alive" to WorkflowValue.IntValue(0),
        ),
    ))
}
