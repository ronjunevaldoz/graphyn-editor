package com.ronjunevaldoz.graphyn.plugins.gmail

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.ShapeCardFactory
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

object GmailEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.gmail.editor",
        displayName = "Gmail Editor",
        version = "0.1.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        // All Gmail nodes use ShapeCard with circular design
        val shapeFactory = ShapeCardFactory(
            shape = CircleShape,
            size = 48.dp
        )

        registrar.registerCanvasCard(GmailNodeSpecs.gmailFetchEmails.type, shapeFactory)
        registrar.registerCanvasCard(GmailNodeSpecs.gmailSendEmail.type, shapeFactory)
        registrar.registerCanvasCard(GmailNodeSpecs.gmailParseEmail.type, shapeFactory)
        registrar.registerCanvasCard(GmailNodeSpecs.gmailGetLabels.type, shapeFactory)
        registrar.registerCanvasCard(GmailNodeSpecs.gmailReplyEmail.type, shapeFactory)

        // Register category
        registrar.registerCategory(
            GmailNodeSpecs.CATEGORY,
            NodeCategoryMeta("Gmail", COLOR_GMAIL, group = SOCIALS_GROUP)
        )
    }
}

private const val SOCIALS_GROUP = "Socials"
private const val COLOR_GMAIL = 0xFF4285F4  // Gmail blue
