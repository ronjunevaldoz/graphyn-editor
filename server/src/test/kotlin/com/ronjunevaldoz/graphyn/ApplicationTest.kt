package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.ExecutionStreamMessage
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class ApplicationTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Real production nodes (json.parse -> json.path), proving the server shares the editor runtime.
    private fun jsonWorkflow() = WorkflowDefinition(
        id = "workflow-server",
        name = "Server",
        nodes = listOf(
            NodeRef(id = "parse", type = "json.parse",
                config = mapOf("text" to WorkflowValue.StringValue("""{"name":"Ada","age":30}"""))),
            NodeRef(id = "name", type = "json.path",
                config = mapOf("path" to WorkflowValue.StringValue("name"))),
        ),
        connections = listOf(
            ConnectionRef(fromNodeId = "parse", fromPort = "value", toNodeId = "name", toPort = "value"),
        ),
    )

    @Test
    fun testRoot() = testApplication {
        application { module() }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Graphyn server is running", response.bodyAsText())
    }

    @Test
    fun testExecuteRunsRealRuntimePlugins() = testApplication {
        application { module() }
        val response = client.post("/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(jsonWorkflow()))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = json.decodeFromString(WorkflowExecutionResult.serializer(), response.bodyAsText())
        assertTrue(result.isFullSuccess, "statuses: ${result.statusByNodeId}")
        assertEquals(WorkflowValue.StringValue("Ada"), result.nodeOutputsByNodeId["name"]?.get("result"))
    }

    @Test
    fun testValidateReportsUnknownNodeType() = testApplication {
        application { module() }
        val broken = WorkflowDefinition(
            id = "broken", name = "Broken",
            nodes = listOf(NodeRef(id = "x", type = "does.not.exist")),
            connections = emptyList(),
        )
        val response = client.post("/validate") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(broken))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val errors = json.decodeFromString<List<ValidationError>>(response.bodyAsText())
        assertTrue(errors.any { it.code == "unknown_node_type" }, "errors: $errors")
    }

    @Test
    fun testExecutionStreamEmitsPerNodeEventsThenResult() = testApplication {
        application { module() }

        val accepted = client.post("/executions") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(jsonWorkflow()))
        }
        assertEquals(HttpStatusCode.Accepted, accepted.status)
        val runId = json.parseToJsonElement(accepted.bodyAsText()).jsonObject["runId"]!!.jsonPrimitive.content

        // The server closes the SSE stream after the terminal frame, so the body completes.
        // Parse the raw text/event-stream: each "data:" line carries one ExecutionStreamMessage.
        val body = client.get("/executions/$runId/events").bodyAsText()
        val frames = body.lineSequence()
            .filter { it.startsWith("data:") }
            .map { json.decodeFromString<ExecutionStreamMessage>(it.removePrefix("data:").trim()) }
            .toList()

        assertTrue(frames.any { it is ExecutionStreamMessage.Event }, "expected per-node events, got $frames")
        val completed = frames.last()
        assertIs<ExecutionStreamMessage.Completed>(completed)
        assertTrue(completed.result.isFullSuccess)
        assertEquals(WorkflowValue.StringValue("Ada"), completed.result.nodeOutputsByNodeId["name"]?.get("result"))
    }
}
