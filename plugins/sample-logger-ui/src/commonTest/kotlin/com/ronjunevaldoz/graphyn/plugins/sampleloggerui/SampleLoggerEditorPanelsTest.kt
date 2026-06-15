package com.ronjunevaldoz.graphyn.plugins.sampleloggerui

import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull

class SampleLoggerEditorPanelsTest {
    @Test
    fun registersPanel() {
        val registry = DefaultEditorPanelRegistry()

        SampleLoggerEditorPanels.register(registry)

        assertNotNull(registry.resolve("sample.logger"))
    }
}
