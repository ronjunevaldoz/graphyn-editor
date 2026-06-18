package com.ronjunevaldoz.graphyn.editor.plugins

import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.editor.canvas.DefaultNodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry

const val GRAPHYN_EDITOR_PLUGIN_API_VERSION = 1

data class GraphynEditorPluginMetadata(
    val id: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
)

interface GraphynEditorPlugin {
    val metadata: GraphynEditorPluginMetadata

    fun register(registrar: GraphynEditorPluginRegistrar)
}

interface GraphynEditorPluginRegistrar {
    val panels: EditorPanelRegistry
    val canvasCards: NodeCanvasRegistry

    fun registerPanel(nodeType: String, factory: EditorPanelFactory) = panels.register(nodeType, factory)
    fun registerCanvasCard(nodeType: String, factory: NodeCanvasFactory) = canvasCards.register(nodeType, factory)
}

interface GraphynEditorPluginRegistry : GraphynEditorPluginRegistrar {
    val plugins: List<GraphynEditorPlugin>

    fun install(plugin: GraphynEditorPlugin)

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
