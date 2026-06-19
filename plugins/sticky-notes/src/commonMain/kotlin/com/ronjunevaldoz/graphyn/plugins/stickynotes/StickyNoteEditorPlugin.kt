package com.ronjunevaldoz.graphyn.plugins.stickynotes

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar

object StickyNoteEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.sticky_note.editor",
        displayName = "Sticky Notes Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(STICKY_NOTE_TYPE, StickyNoteCardFactory)
        registrar.registerCategory(
            STICKY_NOTE_CATEGORY,
            NodeCategoryMeta("Annotations", 0xFFFFCA28L),
        )
    }
}

private object StickyNoteCardFactory : NodeCanvasFactory {
    override val nodeWidth = 200
    override val nodeHeight = 160

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int = 80

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = StickyNoteCard(context)
}
