@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.workflows.*
import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.io.IoPlugin
import com.ronjunevaldoz.graphyn.plugins.json.JsonPlugin
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiIngestionWorkflowTest {

    // Stubs the network/file edges; JSON nodes run for real so the parse→path→stringify chain is exercised.
    private fun stubbedExecutors(captured: MutableMap<String, String>): DefaultNodeExecutorRegistry {
        val executors = DefaultNodeExecutorRegistry()
        DefaultGraphynPluginRegistry(DefaultNodeSpecRegistry(), executors).install(JsonPlugin)
        executors.register("io.http_request") {
            mapOf(
                "body" to WorkflowValue.StringValue("""{"name":"kotlin","stargazers_count":50000,"forks_count":6000}"""),
                "statusCode" to WorkflowValue.IntValue(200),
                "ok" to WorkflowValue.BooleanValue(true),
            )
        }
        executors.register("io.file_write") { inputs ->
            captured["content"] = (inputs["content"] as? WorkflowValue.StringValue)?.value.orEmpty()
            mapOf("success" to WorkflowValue.BooleanValue(true))
        }
        return executors
    }

    @Test
    fun pipelineHasNoValidationErrors() {
        val specs = DefaultGraphynPluginRegistry().apply { installAll(listOf(IoPlugin, JsonPlugin)) }.nodeSpecs
        val errors = WorkflowGraphValidator(specs).validate(apiIngestionDemoWorkflow)
        assertTrue(errors.isEmpty(), "expected a clean pipeline, got: $errors")
    }

    @Test
    fun pipelineFetchesParsesExtractsAndPersists() = runBlocking {
        val captured = mutableMapOf<String, String>()
        val result = WorkflowExecutionEngine(stubbedExecutors(captured))
            .execute(apiIngestionDemoWorkflow)

        // Every node succeeded.
        assertTrue(result.isFullSuccess, "statuses: ${result.statusByNodeId}")
        assertEquals(6, result.successCount)

        // Field extraction by path.
        assertEquals(WorkflowValue.IntValue(50000), result.nodeOutputsByNodeId["stars"]?.get("result"))
        assertEquals(WorkflowValue.IntValue(6000), result.nodeOutputsByNodeId["forks"]?.get("result"))
        assertEquals(WorkflowValue.BooleanValue(true), result.nodeOutputsByNodeId["stars"]?.get("found"))

        // Serialized JSON was persisted through the (stubbed) file writer.
        assertTrue(captured["content"]?.contains("stargazers_count") == true, "persisted: ${captured["content"]}")
        assertEquals(WorkflowValue.BooleanValue(true), result.nodeOutputsByNodeId["save"]?.get("success"))
    }

    @Test
    fun upstreamFailureSkipsDependents() = runBlocking {
        val executors = stubbedExecutors(mutableMapOf())
        // Replace fetch with a failing executor; downstream nodes must be skipped.
        executors.register("io.http_request") { throw IllegalStateException("network down") }

        val result = WorkflowExecutionEngine(executors).execute(apiIngestionDemoWorkflow)

        assertEquals(NodeExecutionStatus.Error, result.statusByNodeId["fetch"])
        assertEquals(NodeExecutionStatus.Skipped, result.statusByNodeId["parse"])
        assertEquals(NodeExecutionStatus.Skipped, result.statusByNodeId["save"])
    }
}
