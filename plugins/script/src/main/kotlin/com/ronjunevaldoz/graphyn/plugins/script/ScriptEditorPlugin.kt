package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

/** Editor plugin: registers the Script node canvas card and palette category. */
object ScriptEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.script.editor",
        displayName = "Kotlin Script Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specScriptEval.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCategory(CATEGORY_SCRIPT, NodeCategoryMeta("Script", 0xFFA78BFAL))
    }
}
