package com.ronjunevaldoz.graphyn.plugins.stickynotes

import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

object StickyNotePlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.sticky_note",
        displayName = "Sticky Notes",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(StickyNoteSpec)
    }
}
