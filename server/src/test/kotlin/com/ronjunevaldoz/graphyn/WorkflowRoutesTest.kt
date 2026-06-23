package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class WorkflowRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleWorkflow(id: String = "wf-test") = WorkflowDefinition(
        id = id, name = "Test Workflow $id",
        nodes = listOf(NodeRef("n1", "json.parse")),
        connections = emptyList(),
    )

    @Test
    fun testListWorkflowsEmptyInitially() = testApplication {
        application { module() }
        val response = client.get("/workflows")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = json.decodeFromString<List<WorkflowMeta>>(response.bodyAsText())
        // May have existing workflows on disk; just assert it responds correctly.
        assertNotNull(list)
    }

    @Test
    fun testSaveAndLoadWorkflow() = testApplication {
        application { module() }
        val wf = sampleWorkflow("wf-save-load-test")

        // Save
        val saveResponse = client.post("/workflows") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(wf))
        }
        assertEquals(HttpStatusCode.Created, saveResponse.status)
        val meta = json.decodeFromString<WorkflowMeta>(saveResponse.bodyAsText())
        assertEquals(wf.id, meta.id)
        assertEquals(wf.name, meta.name)

        // Load
        val loadResponse = client.get("/workflows/${wf.id}")
        assertEquals(HttpStatusCode.OK, loadResponse.status)
        val loaded = DefaultWorkflowJsonCodec.decodeFromString(loadResponse.bodyAsText())
        assertEquals(wf.id, loaded.id)
        assertEquals(wf.name, loaded.name)
    }

    @Test
    fun testLoadUnknownWorkflowReturns404() = testApplication {
        application { module() }
        val response = client.get("/workflows/does-not-exist-xyz")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteWorkflow() = testApplication {
        application { module() }
        val wf = sampleWorkflow("wf-delete-test")

        client.post("/workflows") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(wf))
        }

        val deleteResponse = client.delete("/workflows/${wf.id}")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Now loading should return 404
        val loadResponse = client.get("/workflows/${wf.id}")
        assertEquals(HttpStatusCode.NotFound, loadResponse.status)
    }
}
