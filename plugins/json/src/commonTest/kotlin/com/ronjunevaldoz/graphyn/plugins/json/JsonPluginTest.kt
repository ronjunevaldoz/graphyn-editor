package com.ronjunevaldoz.graphyn.plugins.json

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonPluginTest {

    private fun executors() = DefaultNodeExecutorRegistry().also {
        DefaultGraphynPluginRegistry(DefaultNodeSpecRegistry(), it).install(JsonPlugin)
    }

    @Test
    fun registersThreeSpecs() {
        val specs = DefaultNodeSpecRegistry()
        DefaultGraphynPluginRegistry(specs, DefaultNodeExecutorRegistry()).install(JsonPlugin)
        assertNotNull(specs.resolve("json.parse"))
        assertNotNull(specs.resolve("json.stringify"))
        assertNotNull(specs.resolve("json.path"))
    }

    @Test
    fun parseThenPathExtractsNestedValue() = runTest {
        val exec = executors()
        val parsed = exec.resolve("json.parse")!!.execute(
            mapOf("text" to WorkflowValue.StringValue("""{"user":{"name":"Ada"},"tags":["x","y"]}""")),
        )
        assertEquals(WorkflowValue.BooleanValue(true), parsed["ok"])

        val name = exec.resolve("json.path")!!.execute(
            mapOf("value" to parsed.getValue("value"), "path" to WorkflowValue.StringValue("user.name")),
        )
        assertEquals(WorkflowValue.StringValue("Ada"), name["result"])
        assertEquals(WorkflowValue.BooleanValue(true), name["found"])

        val tag = exec.resolve("json.path")!!.execute(
            mapOf("value" to parsed.getValue("value"), "path" to WorkflowValue.StringValue("tags.1")),
        )
        assertEquals(WorkflowValue.StringValue("y"), tag["result"])
    }

    @Test
    fun parseInvalidJsonReportsNotOk() = runTest {
        val parsed = executors().resolve("json.parse")!!.execute(
            mapOf("text" to WorkflowValue.StringValue("{ not json")),
        )
        assertEquals(WorkflowValue.BooleanValue(false), parsed["ok"])
        assertEquals(WorkflowValue.NullValue, parsed["value"])
    }

    @Test
    fun missingPathReportsNotFound() = runTest {
        val exec = executors()
        val parsed = exec.resolve("json.parse")!!.execute(
            mapOf("text" to WorkflowValue.StringValue("""{"a":1}""")),
        )
        val miss = exec.resolve("json.path")!!.execute(
            mapOf("value" to parsed.getValue("value"), "path" to WorkflowValue.StringValue("a.b.c")),
        )
        assertEquals(WorkflowValue.BooleanValue(false), miss["found"])
        assertEquals(WorkflowValue.NullValue, miss["result"])
    }

    @Test
    fun stringifyRoundTripsThroughParse() = runTest {
        val exec = executors()
        val source = """{"n":42,"ok":true,"list":[1,2]}"""
        val parsed = exec.resolve("json.parse")!!.execute(mapOf("text" to WorkflowValue.StringValue(source)))
        val text = exec.resolve("json.stringify")!!.execute(mapOf("value" to parsed.getValue("value")))
        val reparsed = exec.resolve("json.parse")!!.execute(mapOf("text" to (text.getValue("text"))))
        assertEquals(parsed["value"], reparsed["value"], "round-trip must preserve the value tree")
        assertTrue((text["text"] as WorkflowValue.StringValue).value.contains("\"n\""))
    }
}
