@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.control

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ControlPluginTest {
    @Test
    fun controlPluginRegistersThreeNodeSpecs() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ControlPlugin)
        assertEquals(3, registry.nodeSpecs.all().size)
        assertNotNull(registry.nodeSpecs.resolve("control.branch"))
        assertNotNull(registry.nodeSpecs.resolve("control.merge"))
        assertNotNull(registry.nodeSpecs.resolve("control.loop"))
    }

    @Test
    fun branchRoutesToTruePathWhenConditionIsTrue() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ControlPlugin)
        val executor = registry.nodeExecutors.resolve("control.branch")!!
        val result = executor.execute(
            mapOf(
                "condition" to WorkflowValue.BooleanValue(true),
                "value" to WorkflowValue.StringValue("hello"),
            ),
        )
        assertEquals(WorkflowValue.StringValue("hello"), result["truePath"])
        assertEquals(WorkflowValue.NullValue, result["falsePath"])
    }

    @Test
    fun branchRoutesToFalsePathWhenConditionIsFalse() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ControlPlugin)
        val executor = registry.nodeExecutors.resolve("control.branch")!!
        val result = executor.execute(
            mapOf(
                "condition" to WorkflowValue.BooleanValue(false),
                "value" to WorkflowValue.StringValue("hello"),
            ),
        )
        assertEquals(WorkflowValue.NullValue, result["truePath"])
        assertEquals(WorkflowValue.StringValue("hello"), result["falsePath"])
    }

    @Test
    fun mergeTakesFirstNonNullInput() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ControlPlugin)
        val executor = registry.nodeExecutors.resolve("control.merge")!!
        val result = executor.execute(
            mapOf(
                "a" to WorkflowValue.StringValue("first"),
                "b" to WorkflowValue.StringValue("second"),
            ),
        )
        assertEquals(WorkflowValue.StringValue("first"), result["result"])
    }

    @Test
    fun mergeFallsBackToBWhenAIsNull() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ControlPlugin)
        val executor = registry.nodeExecutors.resolve("control.merge")!!
        val result = executor.execute(
            mapOf(
                "a" to WorkflowValue.NullValue,
                "b" to WorkflowValue.StringValue("fallback"),
            ),
        )
        assertEquals(WorkflowValue.StringValue("fallback"), result["result"])
    }

    @Test
    fun loopEmitsFirstItemAndIndexZero() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(ControlPlugin)
        val executor = registry.nodeExecutors.resolve("control.loop")!!
        val result = executor.execute(
            mapOf("list" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue("a"), WorkflowValue.StringValue("b")))),
        )
        assertEquals(WorkflowValue.StringValue("a"), result["item"])
        assertEquals(WorkflowValue.IntValue(0), result["index"])
    }
}
