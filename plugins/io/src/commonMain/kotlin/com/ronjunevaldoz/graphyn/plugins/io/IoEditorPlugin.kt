package com.ronjunevaldoz.graphyn.plugins.io

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object IoEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.io.editor",
        displayName = "I/O Operations Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specHttpRequest.type, FieldCardFactory(inputRows = 4, outputRows = 3))
        registrar.registerCanvasCard(specFileRead.type, FieldCardFactory(inputRows = 1, outputRows = 2))
        registrar.registerCanvasCard(specFileWrite.type, FieldCardFactory(inputRows = 3, outputRows = 1))
        registrar.registerCategory(CATEGORY_IO, NodeCategoryMeta("I/O", 0xFF34D399L))
    }
}
