package com.ronjunevaldoz.graphyn.plugins.preview

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar

/** Editor plugin: registers the Preview node canvas card and palette category. */
object PreviewEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.preview.editor",
        displayName = "Preview Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(specPreviewView.type, PreviewCardFactory)
        registrar.registerCategory(CATEGORY_PREVIEW, NodeCategoryMeta("Preview", 0xFF94E2D5L))
    }
}
