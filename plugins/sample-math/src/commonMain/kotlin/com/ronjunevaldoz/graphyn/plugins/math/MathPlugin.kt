package com.ronjunevaldoz.graphyn.plugins.math

import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

object MathPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.sample.math",
        displayName = "Sample Math",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(MathNodes.add)
        registrar.registerNodeSpec(MathNodes.subtract)
        registrar.registerNodeSpec(MathNodes.multiply)
        registrar.registerExecutor("math.add", MathExecutors::add)
        registrar.registerExecutor("math.subtract", MathExecutors::subtract)
        registrar.registerExecutor("math.multiply", MathExecutors::multiply)
    }
}
