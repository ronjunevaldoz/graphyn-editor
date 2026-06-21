@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.listops

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ListOpsPluginTest {
    @Test
    fun listOpsPluginRegistersFourNodeSpecs() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ListOpsPlugin)
        assertEquals(4, registry.nodeSpecs.all().size)
        assertNotNull(registry.nodeSpecs.resolve("listops.map"))
        assertNotNull(registry.nodeSpecs.resolve("listops.filter"))
        assertNotNull(registry.nodeSpecs.resolve("listops.reduce"))
        assertNotNull(registry.nodeSpecs.resolve("listops.zip"))
    }

    @Test
    fun mapPassesThroughListUnchanged() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ListOpsPlugin)
        val executor = registry.nodeExecutors.resolve("listops.map")!!
        val list = WorkflowValue.ListValue(listOf(WorkflowValue.IntValue(1), WorkflowValue.IntValue(2)))
        val result = executor.execute(mapOf("list" to list))
        assertEquals(list, result["result"])
    }

    @Test
    fun filterPassesThroughListUnchanged() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ListOpsPlugin)
        val executor = registry.nodeExecutors.resolve("listops.filter")!!
        val list = WorkflowValue.ListValue(listOf(WorkflowValue.StringValue("x")))
        val result = executor.execute(mapOf("list" to list))
        assertEquals(list, result["result"])
    }

    @Test
    fun reduceReturnsInitialWhenNoList() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ListOpsPlugin)
        val executor = registry.nodeExecutors.resolve("listops.reduce")!!
        val initial = WorkflowValue.IntValue(0)
        val result = executor.execute(mapOf("initial" to initial))
        assertEquals(initial, result["result"])
    }

    @Test
    fun reduceReturnsNullWhenNoInputs() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ListOpsPlugin)
        val executor = registry.nodeExecutors.resolve("listops.reduce")!!
        val result = executor.execute(emptyMap())
        assertEquals(WorkflowValue.NullValue, result["result"])
    }

    @Test
    fun zipReturnsEmptyListStub() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ListOpsPlugin)
        val executor = registry.nodeExecutors.resolve("listops.zip")!!
        val result = executor.execute(emptyMap())
        assertEquals(WorkflowValue.ListValue(emptyList()), result["result"])
    }
}
