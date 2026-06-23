package com.ronjunevaldoz.graphyn.pluginapi

import com.ronjunevaldoz.graphyn.core.model.NodeSpec

/**
 * Test fixture: a plugin registered via `META-INF/services` so [discoverGraphynPlugins] can
 * find it on the JVM classpath. Has a public no-arg constructor as ServiceLoader requires.
 */
class DiscoverablePlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "test.discoverable",
        displayName = "Discoverable Test Plugin",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(NodeSpec(type = "test.discovered", label = "Discovered", inputs = emptyList(), outputs = emptyList()))
        registrar.registerExecutor("test.discovered") { it }
    }
}
