@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ApiIngestionProWorkflowTest {
    private fun executors(captured: MutableMap<String, String>): DefaultNodeExecutorRegistry =
        DefaultNodeExecutorRegistry().also { executors ->
            DefaultGraphynPluginRegistry(DefaultNodeSpecRegistry(), executors).installAll(
                GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins,
            )
            executors.register("io.http_request") {
                mapOf(
                    "body" to WorkflowValue.StringValue("""{"full_name":"kotlin/kotlin","stargazers_count":50000,"forks_count":6000,"language":"Kotlin","updated_at":"2026-07-03"}"""),
                    "statusCode" to WorkflowValue.IntValue(200),
                    "ok" to WorkflowValue.BooleanValue(true),
                )
            }
            executors.register("io.file_write") { inputs ->
                captured["path"] = (inputs["path"] as? WorkflowValue.StringValue)?.value.orEmpty()
                captured["content"] = (inputs["content"] as? WorkflowValue.StringValue)?.value.orEmpty()
                mapOf("success" to WorkflowValue.BooleanValue(true))
            }
        }

    @Test
    fun workflowHasNoValidationErrors() {
        val specs = DefaultGraphynPluginRegistry().apply {
            installAll(GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins)
        }.nodeSpecs
        val errors = WorkflowGraphValidator(specs).validate(productIngestionWorkflow)
        assertTrue(errors.isEmpty(), "expected a clean workflow, got: $errors")
    }

    @Test
    fun workflowNormalizesAndPersists() = runBlocking {
        val captured = mutableMapOf<String, String>()
        val result = WorkflowExecutionEngine(executors(captured)).execute(productIngestionWorkflow)

        assertEquals(null, result.errorsByNodeId["normalize"])
        assertTrue(result.isFullSuccess, "statuses: ${result.statusByNodeId}")
        val normalized = result.nodeOutputsByNodeId["normalize"]?.get("result") as WorkflowValue.RecordValue
        assertEquals("kotlin/kotlin", (normalized.fields["full_name"] as WorkflowValue.StringValue).value)
        assertTrue(captured["content"]?.contains("\"stars\": 50000") == true)
        assertEquals("api-ingestion-product.json", captured["path"])
    }
}
