@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.io

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IoPluginTest {
    @Test
    fun ioPluginRegistersSevenNodeSpecs() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        assertEquals(7, registry.nodeSpecs.all().size)
        assertNotNull(registry.nodeSpecs.resolve("io.http_request"))
        assertNotNull(registry.nodeSpecs.resolve("io.file_read"))
        assertNotNull(registry.nodeSpecs.resolve("io.file_write"))
        assertNotNull(registry.nodeSpecs.resolve("io.file_browse"))
        assertNotNull(registry.nodeSpecs.resolve("io.folder_browse"))
        assertNotNull(registry.nodeSpecs.resolve("net.webhook_post"))
        assertNotNull(registry.nodeSpecs.resolve("env.read"))
    }

    @Test
    fun fileReadReturnsNotExistsForMissingFile() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        val executor = registry.nodeExecutors.resolve("io.file_read")!!
        val result = executor.execute(mapOf("path" to WorkflowValue.StringValue("/nonexistent/path/file.txt")))
        assertEquals(WorkflowValue.StringValue(""), result["content"])
        assertEquals(WorkflowValue.BooleanValue(false), result["exists"])
    }

    @Test
    fun fileWriteReturnsFalseForBlankPath() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        val executor = registry.nodeExecutors.resolve("io.file_write")!!
        val result = executor.execute(mapOf("path" to WorkflowValue.StringValue(""), "content" to WorkflowValue.StringValue("data")))
        assertEquals(WorkflowValue.BooleanValue(false), result["success"])
    }

    @Test
    fun envReadReturnsNullForUnknownVar() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        val executor = registry.nodeExecutors.resolve("env.read")!!
        val result = executor.execute(mapOf("name" to WorkflowValue.StringValue("__GRAPHYN_UNDEFINED_VAR_XYZ__")))
        assertEquals(WorkflowValue.NullValue, result["value"])
        assertEquals(WorkflowValue.BooleanValue(false), result["found"])
    }

    @Test
    fun fileBrowsePassesThroughConfiguredPath() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        val executor = registry.nodeExecutors.resolve("io.file_browse")!!
        val path = WorkflowValue.StringValue("/home/user/file.txt")
        val result = executor.execute(mapOf("path" to path))
        assertEquals(path, result["path"])
    }

    @Test
    fun folderBrowsePassesThroughConfiguredPath() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        val executor = registry.nodeExecutors.resolve("io.folder_browse")!!
        val path = WorkflowValue.StringValue("/home/user/dir")
        val result = executor.execute(mapOf("path" to path))
        assertEquals(path, result["path"])
    }

    @Test
    fun fileBrowseReturnsEmptyStringWhenNoPath() = runTest {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(IoPlugin)
        val executor = registry.nodeExecutors.resolve("io.file_browse")!!
        val result = executor.execute(emptyMap())
        assertEquals(WorkflowValue.StringValue(""), result["path"])
    }
}
