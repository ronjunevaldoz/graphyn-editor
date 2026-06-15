package com.ronjunevaldoz.graphyn.plugins.sampleloggerui

import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull

class SampleLoggerEditorPluginTest {
    @Test
    fun registersSamplePanel() {
        val registry = DefaultGraphynEditorPluginRegistry()

        registry.install(SampleLoggerEditorPlugin)

        assertNotNull(registry.panels.resolve("sample.logger"))
    }
}
