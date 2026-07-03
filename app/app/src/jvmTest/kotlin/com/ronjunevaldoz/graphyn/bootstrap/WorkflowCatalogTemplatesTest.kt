@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertFalse("Wan Image to Video (720p)" in names, "media template should be hidden on web")
        assertFalse("Wan Image to Video (480p)" in names, "media template should be hidden on web")
        assertFalse("Wan Image to Video (5B)" in names, "media template should be hidden on web")
        assertFalse("AI Shorts (Image Motion)" in names, "media template should be hidden on web")
        assertFalse("AI Shorts (Video Motion)" in names, "media template should be hidden on web")
        assertFalse("API Ingestion Pro" in names, "script-based ingestion template should be hidden on web")
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
        assertTrue("Wan Image to Video (720p)" in names)
        assertTrue("Wan Image to Video (480p)" in names)
        assertTrue("Wan Image to Video (5B)" in names)
        assertTrue("AI Shorts (Image Motion)" in names)
        assertTrue("AI Shorts (Video Motion)" in names)
        assertTrue("API Ingestion Pro" in names)
    }

    @Test
    fun aiMediaTemplatesSortBeforeOlderMediaTemplates() {
        val desktopRuntime = DefaultGraphynPluginRegistry().apply {
            (GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
        }
        val mediaNames = catalogTemplatesFor(desktopRuntime.nodeSpecs)
            .filter { it.category == WorkflowCategory.Media }
            .map { it.name }

        assertEquals("AI Shorts (Video Motion)", mediaNames.first())
        assertEquals("AI Shorts (Image Motion)", mediaNames[1])
        assertTrue(mediaNames.indexOf("Text to Speech") > mediaNames.indexOf("FLUX Text to Image"))
        assertTrue(mediaNames.indexOf("Wan Image to Video (5B)") < mediaNames.indexOf("Wan Image to Video (480p)"))
        assertTrue(mediaNames.indexOf("Wan Image to Video (480p)") < mediaNames.indexOf("Wan Image to Video (720p)"))
        assertTrue(mediaNames.indexOf("Wan Image to Video (720p)") < mediaNames.indexOf("Qwen Image Edit"))
    }
}
