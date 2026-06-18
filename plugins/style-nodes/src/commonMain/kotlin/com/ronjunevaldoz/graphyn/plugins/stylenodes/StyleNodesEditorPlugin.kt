package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.canvas.NodeShape
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar

object StyleNodesEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.style.nodes.editor",
        displayName = "Style Nodes Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(StyleNodesSpecs.kSampler.type, DarkHeaderCardFactory)
        registrar.registerCanvasCard(StyleNodesSpecs.distributePoints.type, FieldCardFactory)
        registrar.registerCanvasCard(StyleNodesSpecs.webhook.type, CircleCardFactory)
        registrar.registerCategory(CATEGORY_AI, NodeCategoryMeta("AI", COLOR_MODEL))
        registrar.registerCategory(CATEGORY_GEOMETRY, NodeCategoryMeta("Geometry", COLOR_GEOMETRY))
        registrar.registerCategory(CATEGORY_AUTOMATION, NodeCategoryMeta("Automation", COLOR_CONDITIONING))
    }
}

private object DarkHeaderCardFactory : NodeCanvasFactory {
    private const val TOP = 41
    private const val ROW_H = 19

    override val nodeWidth: Int = 200
    override val nodeHeight: Int = 120

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = DarkHeaderCard(context)

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        TOP + portIndex * ROW_H
}

private object FieldCardFactory : NodeCanvasFactory {
    private const val TOP = 36
    private const val ROW_H = 19

    override val nodeWidth: Int = 220
    override val nodeHeight: Int = 120

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = FieldCard(context)

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) TOP + portIndex * ROW_H
        else TOP + spec.inputs.size * ROW_H + portIndex * ROW_H
}

private object CircleCardFactory : NodeCanvasFactory {
    override val nodeWidth: Int = 64
    override val nodeHeight: Int = 64
    override val nodeShape: NodeShape = NodeShape.Circle

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = CircleCard(context)

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int = 32
}
