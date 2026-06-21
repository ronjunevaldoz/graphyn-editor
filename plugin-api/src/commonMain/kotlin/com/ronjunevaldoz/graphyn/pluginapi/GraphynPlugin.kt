package com.ronjunevaldoz.graphyn.pluginapi

import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

/** Current plugin API contract version. Increment when breaking changes are introduced. */
const val GRAPHYN_PLUGIN_API_VERSION = 1

/**
 * Identity and version information declared by a [GraphynPlugin].
 *
 * @param id Reverse-domain identifier unique across all installed plugins, e.g. `"graphyn.io"`.
 * @param displayName Human-readable name shown in the editor's plugin list.
 * @param version Semantic version string of this plugin release, e.g. `"1.0.0"`.
 * @param apiVersion Must match [GRAPHYN_PLUGIN_API_VERSION] or installation will fail.
 */
data class GraphynPluginMetadata(
    val id: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int = GRAPHYN_PLUGIN_API_VERSION,
)

/**
 * Extension point for contributing node specs and executors to the runtime.
 *
 * Implement this interface and install the plugin via [GraphynPluginRegistry.install].
 */
interface GraphynPlugin {
    /** Identity and version of this plugin. */
    val metadata: GraphynPluginMetadata

    /**
     * Called once by the host during installation. Use [registrar] to add node specs
     * and executors. Do not call this method directly — use [GraphynPluginRegistry.install].
     */
    fun register(registrar: GraphynPluginRegistrar)
}

/**
 * Passed to [GraphynPlugin.register] so the plugin can self-register its node specs and executors.
 *
 * ```kotlin
 * override fun register(registrar: GraphynPluginRegistrar) {
 *     registrar.registerNodeSpec(mySpec)
 *     registrar.registerExecutor(mySpec.type) { inputs -> ... }
 * }
 * ```
 */
interface GraphynPluginRegistrar {
    /** Registry that stores node specs contributed by all installed plugins. */
    val nodeSpecs: NodeSpecRegistry

    /** Registry that stores executors contributed by all installed plugins. */
    val nodeExecutors: NodeExecutorRegistry

    /** Registers [spec] so the editor can list and place this node type. */
    fun registerNodeSpec(spec: NodeSpec) {
        nodeSpecs.register(spec)
    }

    /** Registers a [NodeExecutor] implementation for [type]. */
    fun registerExecutor(type: String, executor: NodeExecutor) {
        nodeExecutors.register(type, executor)
    }
}

/** Host-side registry that installs [GraphynPlugin]s and owns the resulting specs and executors. */
interface GraphynPluginRegistry : GraphynPluginRegistrar {
    /** Ordered list of plugins that have been successfully installed. */
    val plugins: List<GraphynPlugin>

    /**
     * Installs [plugin], calling [GraphynPlugin.register] to let it contribute its specs
     * and executors. Throws if the plugin's API version doesn't match or it is already installed.
     */
    fun install(plugin: GraphynPlugin)

    /** Convenience wrapper that calls [install] for each plugin in [plugins]. */
    fun installAll(plugins: Iterable<GraphynPlugin>) {
        for (plugin in plugins) {
            install(plugin)
        }
    }
}

/** In-memory [GraphynPluginRegistry]; suitable for production use in any host. */
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
