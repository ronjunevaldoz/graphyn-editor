package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/** Runtime plugin that registers the Kotlin script executor. JVM/Desktop only. */
object ScriptPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.script",
        displayName = "Kotlin Script",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(specScriptEval)
        registrar.registerExecutor(specScriptEval.type, ScriptExecutor)
    }
}
