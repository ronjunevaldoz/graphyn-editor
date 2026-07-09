package com.ronjunevaldoz.graphyn.plugins.json

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import kotlinx.serialization.json.Json

const val CATEGORY_JSON = "graphyn.json"

private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }
private val prettyJson = Json { prettyPrint = true }

internal val specJsonParse = NodeSpec(
    type = "json.parse",
    label = "JSON Parse",
    description = "Parses a JSON string into a structured value (record/list/primitive).",
    category = CATEGORY_JSON,
    inputs = listOf(PortSpec("text", WorkflowType.StringType, description = "JSON source text")),
    outputs = listOf(
        PortSpec("value", WorkflowType.OpaqueType, description = "Parsed value tree"),
        PortSpec("ok", WorkflowType.BooleanType, description = "True if parsing succeeded"),
        PortSpec(
            "error", WorkflowType.NullableType(WorkflowType.StringType),
            description = "Parse exception message plus a snippet of the offending text if parsing failed, otherwise null",
        ),
    ),
    defaultValues = mapOf("text" to WorkflowValue.StringValue("")),
)

internal val specJsonStringify = NodeSpec(
    type = "json.stringify",
    label = "JSON Stringify",
    description = "Serializes a value back into a JSON string.",
    category = CATEGORY_JSON,
    inputs = listOf(
        PortSpec("value", WorkflowType.OpaqueType, description = "Value to serialize"),
        PortSpec("pretty", WorkflowType.BooleanType, description = "Pretty-print with indentation"),
    ),
    outputs = listOf(PortSpec("text", WorkflowType.StringType, description = "JSON text")),
    defaultValues = mapOf("pretty" to WorkflowValue.BooleanValue(false)),
)

internal val specJsonPath = NodeSpec(
    type = "json.path",
    label = "JSON Path",
    description = "Extracts a nested value by dotted path, e.g. 'items.0.name'.",
    category = CATEGORY_JSON,
    inputs = listOf(
        PortSpec("value", WorkflowType.OpaqueType, description = "Source value tree"),
        PortSpec("path", WorkflowType.StringType, description = "Dotted path (keys and list indices)"),
    ),
    outputs = listOf(
        PortSpec("result", WorkflowType.NullableType(WorkflowType.OpaqueType), description = "Extracted value, or null if absent"),
        PortSpec("found", WorkflowType.BooleanType, description = "True if the path resolved"),
    ),
    defaultValues = mapOf("path" to WorkflowValue.StringValue("")),
)

object JsonPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.json",
        displayName = "JSON",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specJsonParse, specJsonStringify, specJsonPath).forEach { registrar.registerNodeSpec(it) }

        registrar.registerExecutor(specJsonParse.type) { inputs ->
            val text = (inputs["text"] as? WorkflowValue.StringValue)?.value.orEmpty()
            try {
                val value = json.parseToJsonElement(text).toWorkflowValue()
                mapOf("value" to value, "ok" to WorkflowValue.BooleanValue(true), "error" to WorkflowValue.NullValue)
            } catch (e: Exception) {
                val snippet = text.take(200).ifBlank { "<empty>" }
                val reason = e.message ?: e::class.simpleName ?: "JSON parse failed"
                mapOf(
                    "value" to WorkflowValue.NullValue, "ok" to WorkflowValue.BooleanValue(false),
                    "error" to WorkflowValue.StringValue("$reason; input: $snippet"),
                )
            }
        }

        registrar.registerExecutor(specJsonStringify.type) { inputs ->
            val value = inputs["value"] ?: WorkflowValue.NullValue
            val pretty = (inputs["pretty"] as? WorkflowValue.BooleanValue)?.value ?: false
            val codec = if (pretty) prettyJson else json
            mapOf("text" to WorkflowValue.StringValue(codec.encodeToString(value.toJsonElement())))
        }

        registrar.registerExecutor(specJsonPath.type) { inputs ->
            val value = inputs["value"] ?: WorkflowValue.NullValue
            val path = (inputs["path"] as? WorkflowValue.StringValue)?.value.orEmpty()
            val resolved = value.resolvePath(path)
            mapOf(
                "result" to (resolved ?: WorkflowValue.NullValue),
                "found" to WorkflowValue.BooleanValue(resolved != null),
            )
        }
    }
}
