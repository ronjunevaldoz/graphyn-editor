package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.test.*

class JobRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun simpleWorkflow() = WorkflowDefinition(
        id = "job-test", name = "Job Test",
        nodes = listOf(NodeRef("n1", "json.parse",
            config = mapOf("text" to WorkflowValue.StringValue("""{"x":1}""")))),
        connections = emptyList(),
    )

    private fun unknownNodeWorkflow() = WorkflowDefinition(
        id = "bad", name = "Bad",
        nodes = listOf(NodeRef("x", "does.not.exist")),
        connections = emptyList(),
    )

    private suspend fun submitJob(client: HttpClient, workflow: WorkflowDefinition): WorkflowJob =
        json.decodeFromString(client.post("/jobs") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(workflow))
        }.bodyAsText())

    private suspend fun awaitJob(client: HttpClient, id: String): WorkflowJob {
        for (i in 0 until 30) {
            delay(100)
            val job = json.decodeFromString<WorkflowJob>(client.get("/jobs/$id").bodyAsText())
            if (job.state != JobState.QUEUED && job.state != JobState.RUNNING) return job
        }
        error("Job $id did not reach terminal state within 3s")
    }

    @Test
    fun getJobsReturnsEmptyInitially() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val response = client.get("/jobs")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(json.decodeFromString<List<WorkflowJob>>(response.bodyAsText()).isEmpty())
    }

    @Test
    fun submitJobReturns202WithQueuedState() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val response = client.post("/jobs") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.Accepted, response.status)
        val job = json.decodeFromString<WorkflowJob>(response.bodyAsText())
        assertNotNull(job.id)
        assertEquals(JobState.QUEUED, job.state)
    }

    @Test
    fun submitJobWithInvalidWorkflowReturns400() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val response = client.post("/jobs") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(unknownNodeWorkflow()))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun getJobByUnknownIdReturns404() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        assertEquals(HttpStatusCode.NotFound, client.get("/jobs/no-such-id").status)
    }

    @Test
    fun getJobByIdReturnsSubmittedJob() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val id = submitJob(client, simpleWorkflow()).id
        val response = client.get("/jobs/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(id, json.decodeFromString<WorkflowJob>(response.bodyAsText()).id)
    }

    @Test
    fun jobReachesCompletedState() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val id = submitJob(client, simpleWorkflow()).id
        val job = awaitJob(client, id)
        assertEquals(JobState.COMPLETED, job.state)
        assertNotNull(job.result)
        assertNotNull(job.finishedAt)
    }

    @Test
    fun getJobsFilteredByState() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        submitJob(client, simpleWorkflow())
        val response = client.get("/jobs?state=QUEUED")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(json.decodeFromString<List<WorkflowJob>>(response.bodyAsText()).all { it.state == JobState.QUEUED })
    }

    @Test
    fun cancelTerminalJobReturnsConflict() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val id = submitJob(client, simpleWorkflow()).id
        awaitJob(client, id)
        val response = client.request("/jobs/$id") { method = HttpMethod.Delete }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun cancelUnknownJobReturns404() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val response = client.request("/jobs/no-such-id") { method = HttpMethod.Delete }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
