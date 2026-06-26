@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkflowCatalogTemplatesTest {

    @Test
    fun webRuntimeHidesJvmOnlyTemplatesButKeepsCoreOnes() {
        val webRuntime = DefaultGraphynPluginRegistry().apply {
            GraphynDemoPlugins.runtime.forEach { install(it) }
        }
        val names = catalogTemplatesFor(webRuntime.nodeSpecs).map { it.name }

        // media.* and script.eval are JVM-only — not installed in the common runtime.
        assertFalse("Text to Speech" in names, "media template should be hidden on web")
        assertFalse("Captioned Video" in names, "media template should be hidden on web")
        assertFalse("Script" in names, "script template should be hidden on web")
        // Core-node templates remain available.
        assertTrue("List Ops" in names, "core template should be visible on web")
        assertTrue("Subgraph" in names, "subgraph template should be visible on web")
    }

    @Test
    fun desktopRuntimeExposesMediaTemplates() {
        val desktopRuntime = DefaultGraphynPluginRegistry().apply {
            (GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
        }
        val names = catalogTemplatesFor(desktopRuntime.nodeSpecs).map { it.name }

        assertTrue("Text to Speech" in names)
        assertTrue("Picture-in-Picture" in names)
        assertTrue("Slideshow" in names)
    }
}
