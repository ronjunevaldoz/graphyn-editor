package com.ronjunevaldoz.graphyn.plugins.samplelogger

import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

object SampleLoggerPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.sample.logger",
        displayName = "Sample Logger",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(SampleLoggerNodes.log)
        registrar.registerExecutor("sample.logger", SampleLoggerExecutors::log)
    }
}
