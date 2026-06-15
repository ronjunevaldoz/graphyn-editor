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
    fun testExecute() = testApplication {
        application {
            module()
        }
        val workflow = WorkflowDefinition(
            id = "workflow-server",
            name = "Server",
            nodes = listOf(
                NodeRef(id = "switch-1", type = "switch", config = mapOf("enabled" to WorkflowValue.BooleanValue(true))),
                NodeRef(id = "display-1", type = "display"),
            ),
            connections = listOf(
                ConnectionRef(
                    fromNodeId = "switch-1",
                    fromPort = "on",
                    toNodeId = "display-1",
                    toPort = "enabled",
                ),
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

        assertEquals(listOf("switch-1", "display-1"), result.executionOrder)
        assertEquals(
            WorkflowValue.BooleanValue(true),
            result.nodeOutputsByNodeId["display-1"]?.get("state"),
        )
    }
}
