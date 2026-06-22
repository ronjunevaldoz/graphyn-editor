@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class ScriptPluginTest {
    @Test
    fun scriptPluginRegistersSpec() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ScriptPlugin)
        assertNotNull(registry.nodeSpecs.resolve("script.eval"))
    }

    @Test
    fun scriptSpecHasInputAndOutputPorts() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ScriptPlugin)
        val spec = registry.nodeSpecs.resolve("script.eval")!!
        assertEquals(1, spec.inputs.size)
        assertEquals("input", spec.inputs[0].name)
        assertEquals(2, spec.outputs.size)
        assertEquals("result", spec.outputs[0].name)
        assertEquals("error", spec.outputs[1].name)
    }
}
