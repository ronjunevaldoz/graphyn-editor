package com.ronjunevaldoz.graphyn.pluginapi

import java.util.ServiceLoader

actual fun discoverGraphynPlugins(): List<GraphynPlugin> =
    ServiceLoader.load(GraphynPlugin::class.java).iterator().asSequence().toList()
