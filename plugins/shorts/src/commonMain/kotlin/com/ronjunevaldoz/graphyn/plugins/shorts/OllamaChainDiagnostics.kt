package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Renders the ok/error state of each stage in the Ollama request -> parse -> path -> parse chain
 * ([storyboardGeneratorSubgraph]/[comparisonGeneratorSubgraph]) into one line per wired stage, so a
 * validation failure in [storyboardValidateExecutor]/[comparisonValidateExecutor] shows which stage
 * actually broke instead of just the final null. All inputs are optional (see
 * `ollamaChainDiagnosticInputs` in ShortsSubgraphSpecs.kt) — a stage with no diagnostic ports wired
 * is simply omitted rather than reported as unknown.
 */
internal fun ollamaChainDiagnostics(inputs: Map<String, WorkflowValue>): String {
    fun bool(name: String) = (inputs[name] as? WorkflowValue.BooleanValue)?.value
    fun str(name: String) = (inputs[name] as? WorkflowValue.StringValue)?.value

    val stages = listOf(
        Triple("HTTP request", bool("httpOk"), str("httpError")),
        Triple("Outer JSON parse", bool("outerParseOk"), str("outerParseError")),
        Triple("Path 'response'", bool("responseFound"), null),
        Triple("Inner JSON parse", bool("innerParseOk"), str("innerParseError")),
    )

    val lines = stages.mapNotNull { (label, ok, error) ->
        if (ok == null) return@mapNotNull null
        if (ok) "$label: ok" else "$label: FAILED" + (error?.let { " ($it)" } ?: "")
    }
    return lines.ifEmpty { listOf("no chain diagnostics wired") }.joinToString(separator = "; ")
}
