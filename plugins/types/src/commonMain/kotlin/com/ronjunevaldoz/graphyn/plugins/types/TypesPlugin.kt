package com.ronjunevaldoz.graphyn.plugins.types

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val CATEGORY_TYPES = "graphyn.types"

internal val specCast = NodeSpec(
    type = "types.cast",
    label = "Cast",
    description = "Attempts to coerce a value to a target type. Emits null on failure.",
    category = CATEGORY_TYPES,
    inputs = listOf(
        PortSpec("value", WorkflowType.OpaqueType, description = "Value to cast"),
        PortSpec("target", WorkflowType.EnumType(listOf("string", "int", "double", "boolean")), description = "Target type"),
    ),
    outputs = listOf(
        PortSpec("result", WorkflowType.NullableType(WorkflowType.OpaqueType), description = "Casted value, or null if cast failed"),
    ),
    defaultValues = mapOf("target" to WorkflowValue.StringValue("string")),
)

internal val specValidate = NodeSpec(
    type = "types.validate",
    label = "Validate",
    description = "Passes the value through if it matches a predicate; otherwise emits null.",
    category = CATEGORY_TYPES,
    inputs = listOf(
        PortSpec("value", WorkflowType.OpaqueType, description = "Value to validate"),
        PortSpec("schema", WorkflowType.OpaqueType, description = "Validation schema or predicate handle"),
    ),
    outputs = listOf(
        PortSpec("valid", WorkflowType.OpaqueType, description = "Value if valid"),
        PortSpec("error", WorkflowType.NullableType(WorkflowType.StringType), description = "Error message if invalid, else null"),
    ),
)

internal val specSchema = NodeSpec(
    type = "types.schema",
    label = "Schema",
    description = "Builds a record schema definition from field names and types.",
    category = CATEGORY_TYPES,
    inputs = listOf(
        PortSpec("fields", WorkflowType.RecordType(emptyMap()), description = "Field definitions as a record"),
    ),
    outputs = listOf(
        PortSpec("schema", WorkflowType.OpaqueType, description = "Compiled schema handle"),
    ),
)

object TypesPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.types",
        displayName = "Type Utilities",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specCast, specValidate, specSchema).forEach { registrar.registerNodeSpec(it) }
        registrar.registerExecutor(specCast.type) { inputs ->
            mapOf("result" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(specValidate.type) { inputs ->
            mapOf("valid" to (inputs["value"] ?: WorkflowValue.NullValue), "error" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(specSchema.type) { inputs ->
            mapOf("schema" to (inputs["fields"] ?: WorkflowValue.RecordValue(emptyMap())))
        }
    }
}
