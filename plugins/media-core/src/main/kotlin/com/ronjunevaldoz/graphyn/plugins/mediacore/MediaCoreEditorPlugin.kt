package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object MediaCoreEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.media.core.editor",
        displayName = "Media Core Editor",
        version = "0.5.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(MediaCoreSpecs.videoImport.type, FieldCardFactory(inputRows = 1, outputRows = 5))
        registrar.registerCanvasCard(MediaCoreSpecs.audioExtract.type, FieldCardFactory(inputRows = 1, outputRows = 3))
        registrar.registerCanvasCard(MediaCoreSpecs.audioMix.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCanvasCard(MediaCoreSpecs.videoStitch.type, FieldCardFactory(inputRows = 2, outputRows = 3))
        registrar.registerCanvasCard(MediaCoreSpecs.videoEncode.type, FieldCardFactory(inputRows = 5, outputRows = 3))
        registrar.registerCanvasCard(MediaCompositionSpecs.captionOverlay.type, FieldCardFactory(inputRows = 3, outputRows = 2))
        registrar.registerCanvasCard(MediaCompositionSpecs.videoCompose.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCanvasCard(MediaCompositionSpecs.timingController.type, FieldCardFactory(inputRows = 3, outputRows = 1))
        registrar.registerCategory(CATEGORY_MEDIA_VIDEO, NodeCategoryMeta("Media / Video", 0xFF2563EBL))
        registrar.registerCategory(CATEGORY_MEDIA_AUDIO, NodeCategoryMeta("Media / Audio", 0xFF16A34AL))
    }
}
