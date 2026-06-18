package com.ronjunevaldoz.graphyn.plugins.stylenodes

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
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
        with(StyleNodesSpecs) {
            registrar.registerNodeSpec(kSampler)
            registrar.registerNodeSpec(distributePoints)
            registrar.registerNodeSpec(webhook)
        }
        registrar.registerExecutor(StyleNodesSpecs.kSampler.type) { inputs ->
            mapOf("latent" to (inputs["latent"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(StyleNodesSpecs.distributePoints.type) { _ ->
            mapOf("points" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(StyleNodesSpecs.webhook.type) { _ ->
            mapOf("body" to WorkflowValue.RecordValue(emptyMap()))
        }
    }
}
