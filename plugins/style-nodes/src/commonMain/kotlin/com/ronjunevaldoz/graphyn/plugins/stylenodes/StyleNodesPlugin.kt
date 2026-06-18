package com.ronjunevaldoz.graphyn.plugins.stylenodes

import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

object StyleNodesPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.style.nodes",
        displayName = "Style Nodes Demo",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(StyleNodesSpecs.kSampler)
        registrar.registerNodeSpec(StyleNodesSpecs.distributePoints)
        registrar.registerNodeSpec(StyleNodesSpecs.webhook)
    }
}
