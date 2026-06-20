package com.ronjunevaldoz.graphyn.plugins.io

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
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
        registrar.registerCanvasCard(specFileBrowse.type, FileBrowseCardFactory)
        registrar.registerCanvasCard(specFolderBrowse.type, FolderBrowseCardFactory)
        registrar.registerCategory(CATEGORY_IO, NodeCategoryMeta("I/O", 0xFF34D399L))
    }
}

private object FileBrowseCardFactory : NodeCanvasFactory {
    override val nodeWidth = FILE_BROWSE_WIDTH
    override val nodeHeight = FILE_BROWSE_HEIGHT
    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = FileBrowseCard(context)
    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int = 78
}

private object FolderBrowseCardFactory : NodeCanvasFactory {
    override val nodeWidth = FILE_BROWSE_WIDTH
    override val nodeHeight = FILE_BROWSE_HEIGHT
    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = FolderBrowseCard(context)
    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int = 78
}
