@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.types

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TypesPluginTest {
    @Test
    fun typesPluginRegistersThreeNodeSpecs() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TypesPlugin)
        assertEquals(3, registry.nodeSpecs.all().size)
        assertNotNull(registry.nodeSpecs.resolve("types.cast"))
        assertNotNull(registry.nodeSpecs.resolve("types.validate"))
        assertNotNull(registry.nodeSpecs.resolve("types.schema"))
    }

    @Test
    fun castPassesThroughInputValue() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TypesPlugin)
        val executor = registry.nodeExecutors.resolve("types.cast")!!
        val input = WorkflowValue.StringValue("42")
        val result = executor.execute(mapOf("value" to input))
        assertEquals(input, result["result"])
    }

    @Test
    fun castReturnsNullWhenNoInput() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TypesPlugin)
        val executor = registry.nodeExecutors.resolve("types.cast")!!
        val result = executor.execute(emptyMap())
        assertEquals(WorkflowValue.NullValue, result["result"])
    }

    @Test
    fun validatePassesThroughValueAndNullError() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TypesPlugin)
        val executor = registry.nodeExecutors.resolve("types.validate")!!
        val input = WorkflowValue.IntValue(7)
        val result = executor.execute(mapOf("value" to input))
        assertEquals(input, result["valid"])
        assertEquals(WorkflowValue.NullValue, result["error"])
    }

    @Test
    fun schemaPassesThroughFieldsAsSchema() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(TypesPlugin)
        val executor = registry.nodeExecutors.resolve("types.schema")!!
        val fields = WorkflowValue.RecordValue(mapOf("name" to WorkflowValue.StringValue("string")))
        val result = executor.execute(mapOf("fields" to fields))
        assertEquals(fields, result["schema"])
    }
}
