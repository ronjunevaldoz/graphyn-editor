package com.ronjunevaldoz.graphyn.plugins.text

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val CATEGORY_TEXT = "graphyn.text"

internal val specFormat = NodeSpec(
    type = "text.format",
    label = "Format",
    description = "Interpolates a template string with named values from a record.",
    category = CATEGORY_TEXT,
    inputs = listOf(
        PortSpec("template", WorkflowType.StringType, description = "Template with {key} placeholders"),
        PortSpec("values", WorkflowType.RecordType(emptyMap()), description = "Named values to substitute"),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.StringType, description = "Interpolated string")),
    defaultValues = mapOf("template" to WorkflowValue.StringValue("{value}")),
)

internal val specSplit = NodeSpec(
    type = "text.split",
    label = "Split",
    description = "Splits a string into a list of substrings by a delimiter.",
    category = CATEGORY_TEXT,
    inputs = listOf(
        PortSpec("text", WorkflowType.StringType, description = "String to split"),
        PortSpec("delimiter", WorkflowType.StringType, description = "Separator string"),
    ),
    outputs = listOf(PortSpec("parts", WorkflowType.ListType(WorkflowType.StringType), description = "Split substrings")),
    defaultValues = mapOf("delimiter" to WorkflowValue.StringValue(",")),
)

internal val specRegex = NodeSpec(
    type = "text.regex",
    label = "Regex Match",
    description = "Tests a string against a regular expression and returns all matches.",
    category = CATEGORY_TEXT,
    inputs = listOf(
        PortSpec("text", WorkflowType.StringType, description = "Input string"),
        PortSpec("pattern", WorkflowType.StringType, description = "Regular expression pattern"),
    ),
    outputs = listOf(
        PortSpec("matches", WorkflowType.ListType(WorkflowType.StringType), description = "All captured substrings"),
        PortSpec("matched", WorkflowType.BooleanType, description = "True if any match was found"),
    ),
    defaultValues = mapOf("pattern" to WorkflowValue.StringValue(".*")),
)

object TextPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.text",
        displayName = "Text Operations",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specFormat, specSplit, specRegex).forEach { registrar.registerNodeSpec(it) }
        registrar.registerExecutor(specFormat.type) { inputs ->
            val tmpl = (inputs["template"] as? WorkflowValue.StringValue)?.value ?: ""
            val vals = (inputs["values"] as? WorkflowValue.RecordValue)?.fields ?: emptyMap()
            val result = vals.entries.fold(tmpl) { acc, (k, v) ->
                acc.replace("{$k}", (v as? WorkflowValue.StringValue)?.value ?: v.toString())
            }
            mapOf("result" to WorkflowValue.StringValue(result))
        }
        registrar.registerExecutor(specSplit.type) { inputs ->
            val text = (inputs["text"] as? WorkflowValue.StringValue)?.value ?: ""
            val delim = (inputs["delimiter"] as? WorkflowValue.StringValue)?.value ?: ","
            mapOf("parts" to WorkflowValue.ListValue(text.split(delim).map { WorkflowValue.StringValue(it) }))
        }
        registrar.registerExecutor(specRegex.type) { inputs ->
            val text = (inputs["text"] as? WorkflowValue.StringValue)?.value ?: ""
            val pattern = (inputs["pattern"] as? WorkflowValue.StringValue)?.value ?: ".*"
            val matches = Regex(pattern).findAll(text).map { WorkflowValue.StringValue(it.value) }.toList()
            mapOf("matches" to WorkflowValue.ListValue(matches), "matched" to WorkflowValue.BooleanValue(matches.isNotEmpty()))
        }
    }
}
