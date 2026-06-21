@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.text

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextPluginTest {
    @Test
    fun textPluginRegistersThreeNodeSpecs() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TextPlugin)
        assertEquals(3, registry.nodeSpecs.all().size)
        assertNotNull(registry.nodeSpecs.resolve("text.format"))
        assertNotNull(registry.nodeSpecs.resolve("text.split"))
        assertNotNull(registry.nodeSpecs.resolve("text.regex"))
    }

    @Test
    fun formatInterpolatesTemplate() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TextPlugin)
        val executor = registry.nodeExecutors.resolve("text.format")!!
        val result = executor.execute(
            mapOf(
                "template" to WorkflowValue.StringValue("Hello, {name}!"),
                "values" to WorkflowValue.RecordValue(mapOf("name" to WorkflowValue.StringValue("World"))),
            ),
        )
        assertEquals(WorkflowValue.StringValue("Hello, World!"), result["result"])
    }

    @Test
    fun formatReturnsTemplateUnchangedWhenNoValues() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TextPlugin)
        val executor = registry.nodeExecutors.resolve("text.format")!!
        val result = executor.execute(mapOf("template" to WorkflowValue.StringValue("No placeholders")))
        assertEquals(WorkflowValue.StringValue("No placeholders"), result["result"])
    }

    @Test
    fun splitDividesStringByDelimiter() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TextPlugin)
        val executor = registry.nodeExecutors.resolve("text.split")!!
        val result = executor.execute(
            mapOf(
                "text" to WorkflowValue.StringValue("a,b,c"),
                "delimiter" to WorkflowValue.StringValue(","),
            ),
        )
        val parts = (result["parts"] as WorkflowValue.ListValue).items
        assertEquals(3, parts.size)
        assertEquals(WorkflowValue.StringValue("a"), parts[0])
        assertEquals(WorkflowValue.StringValue("c"), parts[2])
    }

    @Test
    fun regexFindsMatches() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TextPlugin)
        val executor = registry.nodeExecutors.resolve("text.regex")!!
        val result = executor.execute(
            mapOf(
                "text" to WorkflowValue.StringValue("foo123bar456"),
                "pattern" to WorkflowValue.StringValue("[0-9]+"),
            ),
        )
        val matches = (result["matches"] as WorkflowValue.ListValue).items
        assertEquals(2, matches.size)
        assertEquals(WorkflowValue.BooleanValue(true), result["matched"])
    }

    @Test
    fun regexReturnsFalseWhenNoMatch() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TextPlugin)
        val executor = registry.nodeExecutors.resolve("text.regex")!!
        val result = executor.execute(
            mapOf(
                "text" to WorkflowValue.StringValue("hello"),
                "pattern" to WorkflowValue.StringValue("[0-9]+"),
            ),
        )
        assertEquals(WorkflowValue.BooleanValue(false), result["matched"])
        val matches = (result["matches"] as WorkflowValue.ListValue).items
        assertTrue(matches.isEmpty())
    }
}
