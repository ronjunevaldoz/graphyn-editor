package com.ronjunevaldoz.graphyn.plugins.types

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.core.model.NodeGroups
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object TypesEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.types.editor",
        displayName = "Type Utilities Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specCast.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specValidate.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCanvasCard(specSchema.type, FieldCardFactory(inputRows = 1, outputRows = 1))
        registrar.registerCategory(CATEGORY_TYPES, NodeCategoryMeta("Types", 0xFF4ADE80L, group = NodeGroups.DATA))
    }
}
