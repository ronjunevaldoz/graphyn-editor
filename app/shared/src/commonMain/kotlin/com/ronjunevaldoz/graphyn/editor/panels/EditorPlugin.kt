package com.ronjunevaldoz.graphyn.editor.panels

interface GraphynEditorPlugin {
    val id: String
    val displayName: String

    fun register(context: GraphynEditorPluginContext)
}

interface GraphynEditorPluginContext {
    val panels: EditorPanelRegistry

    fun registerPanel(nodeType: String, factory: EditorPanelFactory) {
        panels.register(nodeType, factory)
    }
}

class DefaultGraphynEditorPluginContext(
    override val panels: EditorPanelRegistry,
) : GraphynEditorPluginContext
