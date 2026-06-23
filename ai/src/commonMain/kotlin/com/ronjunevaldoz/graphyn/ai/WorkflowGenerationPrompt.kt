package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec

/**
 * Builds the LLM instruction for workflow generation. Pure and testable — no I/O.
 *
 * The model is told the exact JSON schema and the available node catalog, and asked to return
 * only JSON. We keep the catalog compact (type + ports) to fit smaller context windows.
 */
internal object WorkflowGenerationPrompt {

    fun system(catalog: List<NodeSpec>): String = buildString {
        appendLine("You are a workflow graph generator. Output ONLY a JSON object, no prose, no markdown fences.")
        appendLine("Schema:")
        appendLine("""{"id":string,"name":string,"nodes":[{"id":string,"type":string}],"connections":[{"fromNodeId":string,"fromPort":string,"toNodeId":string,"toPort":string}]}""")
        appendLine()
        appendLine("Rules:")
        appendLine("- Use ONLY node types from the catalog below. Never invent a type.")
        appendLine("- Each node id must be unique.")
        appendLine("- A connection's fromPort must be an output port of its fromNode's type; toPort must be an input port of its toNode's type.")
        appendLine("- Prefer a small, correct graph over a large speculative one.")
        appendLine()
        appendLine("Catalog (type: inputs -> outputs):")
        catalog.forEach { spec ->
            val ins = spec.inputs.joinToString(",") { it.name }.ifEmpty { "(none)" }
            val outs = spec.outputs.joinToString(",") { it.name }.ifEmpty { "(none)" }
            appendLine("- ${spec.type}: [$ins] -> [$outs]${spec.description?.let { " — $it" } ?: ""}")
        }
    }

    fun user(prompt: String): String =
        "Build a workflow for this request:\n$prompt\n\nReturn only the JSON object."
}
