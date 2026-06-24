package com.ronjunevaldoz.graphyn.plugins.linkedin

import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.ui.cards.ShapeCardFactory
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

object LinkedInEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.linkedin.editor",
        displayName = "LinkedIn Editor",
        version = "0.2.2",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        val shapeFactory = ShapeCardFactory(
            shape = CircleShape,
            size = 48.dp
        )

        LinkedInNodeSpecs.all.forEach { spec ->
            registrar.registerCanvasCard(spec.type, shapeFactory)
        }

        registrar.registerCategory(
            LINKEDIN_CATEGORY,
            NodeCategoryMeta("LinkedIn", COLOR_LINKEDIN)
        )
    }
}

private const val LINKEDIN_CATEGORY = "graphyn.linkedin"
private const val COLOR_LINKEDIN = 0xFF0A66C2  // LinkedIn blue
