package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistry

data class GraphynServerRuntime(
    val plugins: GraphynPluginRegistry,
    val executionEngine: WorkflowExecutionEngine,
)

fun createGraphynServerRuntime(): GraphynServerRuntime {
    val plugins = DefaultGraphynPluginRegistry()
    plugins.install(GraphynBuiltInPlugin)

    return GraphynServerRuntime(
        plugins = plugins,
        executionEngine = WorkflowExecutionEngine(
            nodeExecutors = plugins.nodeExecutors,
            nodeSpecs = plugins.nodeSpecs,
        ),
    )
}

private object GraphynBuiltInPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.server.builtins",
        displayName = "Graphyn Server Builtins",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(
            NodeSpec(
                type = "switch",
                label = "Switch",
                inputs = listOf(
                    PortSpec(name = "enabled", type = WorkflowType.BooleanType, required = false),
                ),
                outputs = listOf(
                    PortSpec(name = "on", type = WorkflowType.BooleanType),
                ),
            ),
        )
        registrar.registerExecutor("switch") { input ->
            val enabled = input["enabled"] as? WorkflowValue.BooleanValue ?: WorkflowValue.BooleanValue(false)
            mapOf("on" to enabled)
        }

        registrar.registerNodeSpec(
            NodeSpec(
                type = "display",
                label = "Display",
                inputs = listOf(
                    PortSpec(name = "enabled", type = WorkflowType.BooleanType, required = false),
                ),
                outputs = listOf(
                    PortSpec(name = "state", type = WorkflowType.BooleanType),
                ),
            ),
        )
        registrar.registerExecutor("display") { input ->
            val enabled = input["enabled"] as? WorkflowValue.BooleanValue ?: WorkflowValue.BooleanValue(false)
            mapOf("state" to enabled)
        }

        registrar.registerNodeSpec(
            NodeSpec(
                type = "value.constant",
                label = "Constant",
                inputs = emptyList(),
                outputs = listOf(
                    PortSpec(name = "value", type = WorkflowType.OpaqueType, required = false),
                ),
            ),
        )
        registrar.registerExecutor("value.constant") { input ->
            mapOf("value" to (input["value"] ?: WorkflowValue.NullValue))
        }
    }
}
