package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType

/**
 * Builds the LLM instruction for workflow generation. Pure and testable — no I/O.
 *
 * The model is told the exact JSON schema and the available node catalog (with input port types),
 * and asked to return only JSON. Each node may carry a `config` object filling its input ports
 * with literal values; ports fed by a connection should be left out of config.
 */
internal object WorkflowGenerationPrompt {

    fun system(catalog: List<NodeSpec>): String = buildString {
        appendLine("You are a workflow graph generator. Output ONLY a JSON object, no prose, no markdown fences.")
        appendLine("Schema:")
        appendLine("""{"id":string,"name":string,"nodes":[{"id":string,"type":string,"config":{portName:value}}],"connections":[{"fromNodeId":string,"fromPort":string,"toNodeId":string,"toPort":string}],"nodePositions":{"nodeId":{"x":int,"y":int}}}""")
        appendLine()
        appendLine("Rules:")
        appendLine("- Use ONLY node types from the catalog below. Never invent a type.")
        appendLine("- Each node id must be unique.")
        appendLine("- A connection's fromPort must be an output port of its fromNode's type; toPort must be an input port of its toNode's type.")
        appendLine("- Fill each node's \"config\" with concrete literal values for its input ports that are NOT fed by a connection.")
        appendLine("  Match the port's type: string -> \"text\", int -> 42, double -> 1.5, boolean -> true. Omit ports you connect.")
        appendLine("- Include nodePositions for every node so the editor can open the graph without a fallback auto-layout.")
        appendLine("- Prefer a small, correct graph over a large speculative one.")
        appendLine()
        appendLine("Catalog (type — inputs[name:type] -> outputs[name:type]):")
        catalog.forEach { spec ->
            val ins = spec.inputs.joinToString(", ") { "${it.name}:${typeName(it.type)}" }.ifEmpty { "none" }
            val outs = spec.outputs.joinToString(", ") { "${it.name}:${typeName(it.type)}" }.ifEmpty { "none" }
            appendLine("- ${spec.type} — [$ins] -> [$outs]")
        }
    }

    fun user(prompt: String): String =
        "Build a workflow for this request:\n$prompt\n\nReturn only the JSON object, with config values for every unconnected input port."

    /** Short, model-friendly type name for a port. */
    private fun typeName(type: WorkflowType): String = when (type) {
        is WorkflowType.StringType -> "string"
        is WorkflowType.IntType -> "int"
        is WorkflowType.DoubleType -> "double"
        is WorkflowType.BooleanType -> "boolean"
        is WorkflowType.ListType -> "list"
        is WorkflowType.NullableType -> typeName(type.wrappedType) + "?"
        is WorkflowType.RecordType -> "record"
        is WorkflowType.EnumType -> "enum(${type.values.joinToString("|")})"
        is WorkflowType.MultiEnumType -> "multienum(${type.values.joinToString("|")})"
        else -> "string"
    }
}
