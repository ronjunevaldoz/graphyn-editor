package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
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
        registrar.registerCanvasCard(StyleNodesSpecs.comfyKSampler.type, ComfyFactory)
        registrar.registerCanvasCard(StyleNodesSpecs.blenderDistribute.type, BlenderFactory)
        registrar.registerCanvasCard(StyleNodesSpecs.n8nWebhook.type, N8nFactory)
    }
}

private object ComfyFactory : NodeCanvasFactory {
    // Header: 12sp text (~15dp line height) + 6dp*2 padding = 27dp
    // Port section: 4dp column top + 3dp row top + 10sp center (~6.5dp) = 13.5dp → total ~41dp
    // Row stride: 3dp top + 13dp line height + 3dp bottom = 19dp
    private const val TOP = 41
    private const val ROW_H = 19

    override val nodeWidth: Int = 200

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = ComfyUiNodeCard(context)

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) {
            TOP + portIndex * ROW_H
        } else {
            TOP + spec.inputs.size * ROW_H + portIndex * ROW_H
        }
}

private object BlenderFactory : NodeCanvasFactory {
    // Header: 11sp text (~14dp line height) + 5dp*2 padding = 24dp
    // Port section: 2dp column top + 3dp row top + 10sp center (~6.5dp) = 11.5dp → total ~36dp
    // Row stride: 3dp top + 13dp line height + 3dp bottom = 19dp
    private const val TOP = 36
    private const val ROW_H = 19

    override val nodeWidth: Int = 220

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = BlenderNodeCard(context)

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) {
            TOP + portIndex * ROW_H
        } else {
            TOP + spec.inputs.size * ROW_H + portIndex * ROW_H
        }
}

private object N8nFactory : NodeCanvasFactory {
    // Circle 64dp, center at 32dp
    override val nodeWidth: Int = 64

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = N8nNodeCard(context)

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int = 32
}
