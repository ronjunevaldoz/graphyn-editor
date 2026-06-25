package com.ronjunevaldoz.graphyn.plugins.text

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.canvas.NodeGroups
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object TextEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.text.editor",
        displayName = "Text Operations Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specFormat.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specSplit.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specRegex.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCategory(CATEGORY_TEXT, NodeCategoryMeta("Text", 0xFF818CF8L, group = NodeGroups.DATA))
    }
}
