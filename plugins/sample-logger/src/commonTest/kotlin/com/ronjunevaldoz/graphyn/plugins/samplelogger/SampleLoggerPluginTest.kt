package com.ronjunevaldoz.graphyn.plugins.samplelogger

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SampleLoggerPluginTest {
    @Test
    fun installsAndExecutes() {
        val registry = DefaultGraphynPluginRegistry()

        registry.install(SampleLoggerPlugin)

        val spec = registry.nodeSpecs.resolve("sample.logger")
        val executor = registry.nodeExecutors.resolve("sample.logger")

        assertNotNull(spec)
        assertNotNull(executor)
        assertEquals(
            WorkflowValue.StringValue("hello"),
            executor.execute(
                mapOf("message" to WorkflowValue.StringValue("hello")),
            )["message"],
        )
    }
}
