package com.ronjunevaldoz.graphyn.plugins.preview

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION

/** Runtime plugin: registers [specPreviewView] and its pass-through executor. */
object PreviewPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.preview",
        displayName = "Preview",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(specPreviewView)
        registrar.registerExecutor(specPreviewView.type) { input ->
            mapOf("value" to (input["value"] ?: WorkflowValue.NullValue))
        }

        registrar.registerNodeSpec(specMediaFileOutput)
        registrar.registerExecutor(specMediaFileOutput.type) { input ->
            mapOf("file_path" to (input["file_path"] ?: WorkflowValue.StringValue("")))
        }
    }
}
