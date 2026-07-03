@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun scriptExecutorPreservesNestedListAndRecordValues() = runBlocking {
        val registry = DefaultGraphynPluginRegistry().apply { install(ScriptPlugin) }
        val result = registry.nodeExecutors.resolve("script.eval")!!.execute(
            mapOf(
                "code" to WorkflowValue.StringValue(
                    """
                    listOf(
                        mapOf("text" to "hello", "start_ms" to 0.0, "end_ms" to 1000.0),
                        mapOf("text" to "world", "start_ms" to 1000.0, "end_ms" to 2000.0),
                    )
                    """.trimIndent(),
                ),
            ),
        )
        val items = (result["result"] as WorkflowValue.ListValue).items
        assertTrue(items.all { it is WorkflowValue.RecordValue })
        assertEquals("hello", ((items[0] as WorkflowValue.RecordValue).fields["text"] as WorkflowValue.StringValue).value)
    }
}
