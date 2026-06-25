package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

object MediaAiEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.media.ai.editor",
        displayName = "Media AI Editor",
        version = "0.5.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(MediaAiSpecs.textToSpeech.type, FieldCardFactory(inputRows = 4, outputRows = 3))
        registrar.registerCanvasCard(MediaAiSpecs.captionStyle.type, FieldCardFactory(inputRows = 4, outputRows = 1))
        registrar.registerCanvasCard(MediaAiSpecs.speechToText.type, FieldCardFactory(inputRows = 2, outputRows = 3))
        registrar.registerCanvasCard(MediaAiSpecs.ocr.type, FieldCardFactory(inputRows = 2, outputRows = 2))
        registrar.registerCategory(CATEGORY_MEDIA_AI, NodeCategoryMeta("Media / AI", 0xFFEA580CL))
    }
}
