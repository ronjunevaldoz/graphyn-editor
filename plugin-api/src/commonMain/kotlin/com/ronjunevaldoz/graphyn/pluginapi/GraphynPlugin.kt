package com.ronjunevaldoz.graphyn.pluginapi

import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

const val GRAPHYN_PLUGIN_API_VERSION = 1

data class GraphynPluginMetadata(
    val id: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int = GRAPHYN_PLUGIN_API_VERSION,
)

interface GraphynPlugin {
    val metadata: GraphynPluginMetadata

    fun register(registrar: GraphynPluginRegistrar)
}

interface GraphynPluginRegistrar {
    val nodeSpecs: NodeSpecRegistry
    val nodeExecutors: NodeExecutorRegistry

    fun registerNodeSpec(spec: NodeSpec) {
        nodeSpecs.register(spec)
    }

    fun registerExecutor(type: String, executor: NodeExecutor) {
        nodeExecutors.register(type, executor)
    }
}

interface GraphynPluginRegistry : GraphynPluginRegistrar {
    val plugins: List<GraphynPlugin>

    fun install(plugin: GraphynPlugin)

    fun installAll(plugins: Iterable<GraphynPlugin>) {
        for (plugin in plugins) {
            install(plugin)
        }
    }
}

@GraphynExperimentalApi
class DefaultGraphynPluginRegistry(
    override val nodeSpecs: NodeSpecRegistry = DefaultNodeSpecRegistry(),
    override val nodeExecutors: NodeExecutorRegistry = DefaultNodeExecutorRegistry(),
) : GraphynPluginRegistry {
    private val installedPluginsById = linkedMapOf<String, GraphynPlugin>()

    override val plugins: List<GraphynPlugin>
        get() = installedPluginsById.values.toList()

    override fun install(plugin: GraphynPlugin) {
        require(plugin.metadata.apiVersion == GRAPHYN_PLUGIN_API_VERSION) {
            "Plugin '${plugin.metadata.id}' targets API ${plugin.metadata.apiVersion}, " +
                "but Graphyn expects $GRAPHYN_PLUGIN_API_VERSION."
        }
        require(plugin.metadata.id !in installedPluginsById) {
            "Plugin '${plugin.metadata.id}' is already installed."
        }

        plugin.register(this)
        installedPluginsById[plugin.metadata.id] = plugin
    }
}
