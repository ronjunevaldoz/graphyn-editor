@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.preview

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PreviewPluginTest {
    @Test
    fun previewPluginRegistersSpec() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(PreviewPlugin)
        assertNotNull(registry.nodeSpecs.resolve("preview.view"))
    }

    @Test
    fun previewExecutorPassesThroughValue() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(PreviewPlugin)
        val executor = registry.nodeExecutors.resolve("preview.view")!!
        val result = executor.execute(mapOf("value" to WorkflowValue.StringValue("hello")))
        assertEquals(WorkflowValue.StringValue("hello"), result["value"])
    }

    @Test
    fun previewExecutorReturnsNullValueWhenInputMissing() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(PreviewPlugin)
        val executor = registry.nodeExecutors.resolve("preview.view")!!
        val result = executor.execute(emptyMap())
        assertEquals(WorkflowValue.NullValue, result["value"])
    }
}
