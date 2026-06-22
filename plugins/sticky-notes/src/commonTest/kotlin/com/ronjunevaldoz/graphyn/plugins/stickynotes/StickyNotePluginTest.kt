@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.stickynotes

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StickyNotePluginTest {
    @Test
    fun stickyNotePluginRegistersSpec() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(StickyNotePlugin)
        assertNotNull(registry.nodeSpecs.resolve("graphyn.sticky_note"))
    }

    @Test
    fun stickyNoteSpecHasNoPorts() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(StickyNotePlugin)
        val spec = registry.nodeSpecs.resolve("graphyn.sticky_note")!!
        assertEquals(emptyList(), spec.inputs)
        assertEquals(emptyList(), spec.outputs)
    }

    @Test
    fun stickyNoteSpecHasEmptyTextDefault() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(StickyNotePlugin)
        val spec = registry.nodeSpecs.resolve("graphyn.sticky_note")!!
        assertEquals(WorkflowValue.StringValue(""), spec.defaultValues[STICKY_NOTE_TEXT_KEY])
    }
}
