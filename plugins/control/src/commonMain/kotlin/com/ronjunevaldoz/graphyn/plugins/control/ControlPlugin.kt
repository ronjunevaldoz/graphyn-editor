package com.ronjunevaldoz.graphyn.plugins.control

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val CATEGORY_CONTROL = "graphyn.control"

internal val specBranch = NodeSpec(
    type = "control.branch",
    label = "Branch",
    description = "Routes the input value to one of two outputs based on a boolean condition.",
    category = CATEGORY_CONTROL,
    inputs = listOf(
        PortSpec("condition", WorkflowType.BooleanType, description = "Routing condition"),
        PortSpec("value", WorkflowType.OpaqueType, description = "Value to route"),
    ),
    outputs = listOf(
        PortSpec("truePath", WorkflowType.OpaqueType, description = "Emitted when condition is true"),
        PortSpec("falsePath", WorkflowType.OpaqueType, description = "Emitted when condition is false"),
    ),
    defaultValues = mapOf("condition" to WorkflowValue.BooleanValue(true)),
)

internal val specMerge = NodeSpec(
    type = "control.merge",
    label = "Merge",
    description = "Passes through whichever of the two inputs is non-null first.",
    category = CATEGORY_CONTROL,
    inputs = listOf(
        PortSpec("a", WorkflowType.OpaqueType, description = "Primary input"),
        PortSpec("b", WorkflowType.OpaqueType, description = "Fallback input"),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType, description = "First non-null input")),
)

internal val specLoop = NodeSpec(
    type = "control.loop",
    label = "Loop",
    description = "Iterates over a list and emits each element as a separate value.",
    category = CATEGORY_CONTROL,
    inputs = listOf(
        PortSpec("list", WorkflowType.ListType(WorkflowType.OpaqueType), description = "List to iterate"),
    ),
    outputs = listOf(
        PortSpec("item", WorkflowType.OpaqueType, description = "Current element"),
        PortSpec("index", WorkflowType.IntType, description = "Zero-based index of the current element"),
    ),
)

object ControlPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.control",
        displayName = "Control Flow",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specBranch, specMerge, specLoop).forEach { registrar.registerNodeSpec(it) }
        registrar.registerExecutor(specBranch.type) { inputs ->
            val cond = (inputs["condition"] as? WorkflowValue.BooleanValue)?.value ?: true
            val v = inputs["value"] ?: WorkflowValue.NullValue
            if (cond) mapOf("truePath" to v, "falsePath" to WorkflowValue.NullValue)
            else mapOf("truePath" to WorkflowValue.NullValue, "falsePath" to v)
        }
        registrar.registerExecutor(specMerge.type) { inputs ->
            val result = inputs["a"].takeUnless { it == WorkflowValue.NullValue || it == null }
                ?: inputs["b"] ?: WorkflowValue.NullValue
            mapOf("result" to result)
        }
        registrar.registerExecutor(specLoop.type) { inputs ->
            val list = (inputs["list"] as? WorkflowValue.ListValue)?.items ?: emptyList()
            mapOf("item" to (list.firstOrNull() ?: WorkflowValue.NullValue), "index" to WorkflowValue.IntValue(0))
        }
    }
}
