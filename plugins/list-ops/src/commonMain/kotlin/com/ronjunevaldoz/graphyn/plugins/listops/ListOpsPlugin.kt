package com.ronjunevaldoz.graphyn.plugins.listops

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val CATEGORY_LIST_OPS = "graphyn.list"

private val listType = WorkflowType.ListType(WorkflowType.OpaqueType)

internal val specMap = NodeSpec(
    type = "listops.map",
    label = "Map",
    description = "Applies a transformation to each element of the list.",
    category = CATEGORY_LIST_OPS,
    inputs = listOf(
        PortSpec("list", listType, description = "Input list"),
        PortSpec("transform", WorkflowType.OpaqueType, description = "Transformation function handle"),
    ),
    outputs = listOf(PortSpec("result", listType, description = "Transformed list")),
)

internal val specFilter = NodeSpec(
    type = "listops.filter",
    label = "Filter",
    description = "Keeps only elements matching a predicate.",
    category = CATEGORY_LIST_OPS,
    inputs = listOf(
        PortSpec("list", listType, description = "Input list"),
        PortSpec("predicate", WorkflowType.OpaqueType, description = "Predicate function handle"),
    ),
    outputs = listOf(PortSpec("result", listType, description = "Filtered list")),
)

internal val specReduce = NodeSpec(
    type = "listops.reduce",
    label = "Reduce",
    description = "Folds a list into a single value using an accumulator.",
    category = CATEGORY_LIST_OPS,
    inputs = listOf(
        PortSpec("list", listType, description = "Input list"),
        PortSpec("initial", WorkflowType.OpaqueType, description = "Initial accumulator value"),
        PortSpec("reducer", WorkflowType.OpaqueType, description = "Reducer function handle"),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType, description = "Accumulated result")),
)

internal val specZip = NodeSpec(
    type = "listops.zip",
    label = "Zip",
    description = "Pairs elements from two lists into a list of records.",
    category = CATEGORY_LIST_OPS,
    inputs = listOf(
        PortSpec("listA", listType, description = "First list"),
        PortSpec("listB", listType, description = "Second list"),
    ),
    outputs = listOf(PortSpec("result", listType, description = "Zipped list of {a, b} records")),
)

object ListOpsPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.list_ops",
        displayName = "List Operations",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specMap, specFilter, specReduce, specZip).forEach { registrar.registerNodeSpec(it) }
        registrar.registerExecutor(specMap.type) { inputs ->
            mapOf("result" to (inputs["list"] ?: WorkflowValue.ListValue(emptyList())))
        }
        registrar.registerExecutor(specFilter.type) { inputs ->
            mapOf("result" to (inputs["list"] ?: WorkflowValue.ListValue(emptyList())))
        }
        registrar.registerExecutor(specReduce.type) { inputs ->
            mapOf("result" to (inputs["initial"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(specZip.type) { _ ->
            mapOf("result" to WorkflowValue.ListValue(emptyList()))
        }
    }
}
