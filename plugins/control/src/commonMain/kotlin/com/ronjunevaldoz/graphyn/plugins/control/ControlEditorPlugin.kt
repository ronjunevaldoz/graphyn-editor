package com.ronjunevaldoz.graphyn.plugins.control

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object ControlEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.control.editor",
        displayName = "Control Flow Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specBranch.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCanvasCard(specMerge.type, FieldCardFactory(inputRows = 2, outputRows = 1))
        registrar.registerCanvasCard(specLoop.type, FieldCardFactory(inputRows = 1, outputRows = 2))
        registrar.registerCategory(CATEGORY_CONTROL, NodeCategoryMeta("Control", 0xFFF9A825L))
    }
}
