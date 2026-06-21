package com.ronjunevaldoz.graphyn.editor.plugins

import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.editor.canvas.DefaultNodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.DefaultNodeCategoryRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryRegistry
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry

const val GRAPHYN_EDITOR_PLUGIN_API_VERSION = 1

/**
 * Identity and version information declared by a [GraphynEditorPlugin].
 *
 * @param id Reverse-domain identifier unique across all installed editor plugins.
 * @param displayName Human-readable name shown in the editor's plugin list.
 * @param version Semantic version string of this plugin release, e.g. `"1.0.0"`.
 * @param apiVersion Must match [GRAPHYN_EDITOR_PLUGIN_API_VERSION] or installation will fail.
 */
data class GraphynEditorPluginMetadata(
    val id: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
)

/**
 * Extension point for adding editor-level capabilities (panels, canvas cards, node categories).
 *
 * Implement this interface and install the plugin via [GraphynEditorPluginRegistry.install].
 */
interface GraphynEditorPlugin {
    /** Identity and version of this editor plugin. */
    val metadata: GraphynEditorPluginMetadata

    /**
     * Called once by the host during installation. Use [registrar] to contribute panels,
     * canvas cards, and categories. Do not call directly — use [GraphynEditorPluginRegistry.install].
     */
    fun register(registrar: GraphynEditorPluginRegistrar)
}

/**
 * Passed to [GraphynEditorPlugin.register] so the plugin can self-register its components.
 *
 * ```kotlin
 * override fun register(registrar: GraphynEditorPluginRegistrar) {
 *     registrar.registerCanvasCard(mySpec.type, MyCardFactory)
 *     registrar.registerCategory(CATEGORY_ID, NodeCategoryMeta("My Nodes", 0xFF6366F1L))
 * }
 * ```
 */
interface GraphynEditorPluginRegistrar {
    /** Panel registry shared across all editor plugins. */
    val panels: EditorPanelRegistry
    /** Canvas card registry shared across all editor plugins. */
    val canvasCards: NodeCanvasRegistry
    /** Category registry shared across all editor plugins. */
    val categories: NodeCategoryRegistry

    /** Registers a custom inspector panel for [nodeType]. */
    fun registerPanel(nodeType: String, factory: EditorPanelFactory) = panels.register(nodeType, factory)
    /** Registers a custom canvas card renderer for [nodeType]. */
    fun registerCanvasCard(nodeType: String, factory: NodeCanvasFactory) = canvasCards.register(nodeType, factory)
    /** Registers display metadata for a node category used by the node-picker palette. */
    fun registerCategory(id: String, meta: NodeCategoryMeta) = categories.register(id, meta)
}

/** Host-side registry that installs [GraphynEditorPlugin]s and owns the resulting registrars. */
interface GraphynEditorPluginRegistry : GraphynEditorPluginRegistrar {
    /** Ordered list of editor plugins that have been successfully installed. */
    val plugins: List<GraphynEditorPlugin>

    /**
     * Installs [plugin], calling [GraphynEditorPlugin.register] to let it contribute its components.
     * Throws if the plugin's API version doesn't match or it is already installed.
     */
    fun install(plugin: GraphynEditorPlugin)

    /** Convenience wrapper that calls [install] for each plugin in [plugins]. */
    fun installAll(plugins: Iterable<GraphynEditorPlugin>) {
        for (plugin in plugins) {
            install(plugin)
        }
    }
}

@GraphynExperimentalApi
class DefaultGraphynEditorPluginRegistry(
    override val panels: EditorPanelRegistry = DefaultEditorPanelRegistry(),
    override val canvasCards: NodeCanvasRegistry = DefaultNodeCanvasRegistry(),
    override val categories: NodeCategoryRegistry = DefaultNodeCategoryRegistry(),
) : GraphynEditorPluginRegistry {
    private val installedPluginsById = linkedMapOf<String, GraphynEditorPlugin>()

    override val plugins: List<GraphynEditorPlugin>
        get() = installedPluginsById.values.toList()

    override fun install(plugin: GraphynEditorPlugin) {
        require(plugin.metadata.apiVersion == GRAPHYN_EDITOR_PLUGIN_API_VERSION) {
            "Editor plugin '${plugin.metadata.id}' targets API ${plugin.metadata.apiVersion}, " +
                "but Graphyn expects $GRAPHYN_EDITOR_PLUGIN_API_VERSION."
        }
        require(plugin.metadata.id !in installedPluginsById) {
            "Editor plugin '${plugin.metadata.id}' is already installed."
        }

        plugin.register(this)
        installedPluginsById[plugin.metadata.id] = plugin
    }
}
