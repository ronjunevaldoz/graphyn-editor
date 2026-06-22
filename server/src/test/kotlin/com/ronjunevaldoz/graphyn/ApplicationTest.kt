package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Graphyn server is running", response.bodyAsText())
    }

    @Test
    fun testExecuteRunsRealRuntimePlugins() = testApplication {
        application {
            module()
        }
        // Uses real production nodes (json.parse -> json.path), proving the server shares the
        // editor's runtime instead of toy built-ins.
        val workflow = WorkflowDefinition(
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

        val response = client.post("/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(workflow))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = Json.decodeFromString(
            WorkflowExecutionResult.serializer(),
            response.bodyAsText(),
        )

        assertTrue(result.isFullSuccess, "statuses: ${result.statusByNodeId}")
        assertEquals(WorkflowValue.BooleanValue(true), result.nodeOutputsByNodeId["parse"]?.get("ok"))
        assertEquals(WorkflowValue.StringValue("Ada"), result.nodeOutputsByNodeId["name"]?.get("result"))
    }
}
