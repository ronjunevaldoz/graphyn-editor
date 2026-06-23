package com.ronjunevaldoz.graphyn.pluginapi

import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(GraphynExperimentalApi::class)
class PluginDiscoveryTest {

    @Test
    fun serviceLoaderFindsRegisteredPlugin() {
        val found = discoverGraphynPlugins()
        assertTrue(
            found.any { it.metadata.id == "test.discoverable" },
            "ServiceLoader should find DiscoverablePlugin; found ${found.map { it.metadata.id }}",
        )
    }

    @Test
    fun installDiscoveredRegistersSpecsAndExecutors() {
        val registry = DefaultGraphynPluginRegistry()
        val installed = registry.installDiscovered()

        assertTrue(installed.any { it.metadata.id == "test.discoverable" })
        assertEquals("Discovered", registry.nodeSpecs.resolve("test.discovered")?.label)
        assertTrue(registry.nodeExecutors.resolve("test.discovered") != null)
    }

    @Test
    fun installDiscoveredSkipsAlreadyInstalledById() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(DiscoverablePlugin())

        // Second pass must not throw "already installed" — it filters by metadata id.
        val newlyInstalled = registry.installDiscovered()
        assertTrue(newlyInstalled.none { it.metadata.id == "test.discoverable" })
    }
}
