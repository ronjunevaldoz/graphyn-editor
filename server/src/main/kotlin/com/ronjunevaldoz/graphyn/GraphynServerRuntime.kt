@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistry
import com.ronjunevaldoz.graphyn.runtime.GraphynRuntime

/**
 * The server's view of the Graphyn runtime: the same production plugin set the editor uses
 * ([GraphynRuntime.runtimePlugins]), so a workflow built and validated in the app runs
 * identically here. No demo or sample nodes are installed server-side.
 */
data class GraphynServerRuntime(
    val plugins: GraphynPluginRegistry,
    val executionEngine: WorkflowExecutionEngine,
    val validator: WorkflowGraphValidator,
)

fun createGraphynServerRuntime(extraPlugins: List<GraphynPlugin> = emptyList()): GraphynServerRuntime {
    val plugins = DefaultGraphynPluginRegistry()
    plugins.installAll(GraphynRuntime.runtimePlugins + extraPlugins)

    return GraphynServerRuntime(
        plugins = plugins,
        executionEngine = WorkflowExecutionEngine(
            nodeExecutors = plugins.nodeExecutors,
            nodeSpecs = plugins.nodeSpecs,
        ),
        validator = WorkflowGraphValidator(plugins.nodeSpecs),
    )
}
