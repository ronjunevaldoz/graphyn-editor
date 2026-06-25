package com.ronjunevaldoz.graphyn.plugins.json

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.core.model.NodeGroups
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object JsonEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.json.editor",
        displayName = "JSON Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specJsonParse.type, FieldCardFactory(inputRows = 1, outputRows = 2))
        registrar.registerCanvasCard(specJsonStringify.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specJsonPath.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCategory(CATEGORY_JSON, NodeCategoryMeta("JSON", 0xFFFBBF24L, group = NodeGroups.DATA))
    }
}
