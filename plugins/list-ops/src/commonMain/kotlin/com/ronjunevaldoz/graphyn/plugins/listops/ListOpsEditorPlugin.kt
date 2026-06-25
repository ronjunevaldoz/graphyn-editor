package com.ronjunevaldoz.graphyn.plugins.listops

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.canvas.NodeGroups
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object ListOpsEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.list_ops.editor",
        displayName = "List Operations Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specMap.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specFilter.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specReduce.type, FieldCardFactory(inputRows = 3, outputRows = 1))
        registrar.registerCanvasCard(specZip.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCategory(CATEGORY_LIST_OPS, NodeCategoryMeta("List Ops", 0xFF38BDF8L, group = NodeGroups.DATA))
    }
}
