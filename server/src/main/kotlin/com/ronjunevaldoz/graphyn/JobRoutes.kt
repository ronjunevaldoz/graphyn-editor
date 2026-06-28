package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

private val APP_JSON = ContentType.Application.Json

/**
 * Async job execution API:
 *
 * - `POST   /jobs`          → [WorkflowJob] (202) — validates then queues for background execution
 * - `GET    /jobs`          → `[WorkflowJob]` list, newest first; `?state=RUNNING` to filter
 * - `GET    /jobs/{id}`     → [WorkflowJob] (404 when unknown)
 * - `DELETE /jobs/{id}`     → 204 cancels a QUEUED/RUNNING job; 409 if already terminal
 *
 * Unlike `POST /executions`, jobs persist in [store] after completion and are queryable by id.
 * [scope] must outlive individual requests — pass the application-level scope.
 */
fun Route.jobRoutes(
    runtime: GraphynServerRuntime,
    store: JobStore,
    scope: CoroutineScope,
    json: Json,
) {
    post("/jobs") {
        val workflow = DefaultWorkflowJsonCodec.decodeFromString(call.receiveText())
        val errors = runtime.validator.validate(workflow)
        if (errors.isNotEmpty()) {
            call.respondText(json.encodeToString<List<ValidationError>>(errors), APP_JSON, HttpStatusCode.BadRequest)
            return@post
        }
        val id = UUID.randomUUID().toString()
        val job = WorkflowJob(id = id, workflowId = workflow.id, state = JobState.QUEUED,
                              submittedAt = System.currentTimeMillis())
        val handle = scope.launch {
            store.update(id) { it.copy(state = JobState.RUNNING, startedAt = System.currentTimeMillis()) }
            try {
                val result = runtime.executionEngine.execute(workflow)
                store.update(id) { it.copy(state = JobState.COMPLETED, finishedAt = System.currentTimeMillis(), result = result) }
            } catch (e: CancellationException) {
                store.update(id) { it.copy(state = JobState.CANCELLED, finishedAt = System.currentTimeMillis()) }
                throw e
            } catch (e: Exception) {
                store.update(id) { it.copy(state = JobState.FAILED, finishedAt = System.currentTimeMillis(), error = e.message) }
            }
        }
        store.add(job, handle)
        call.respondText(json.encodeToString(job), APP_JSON, HttpStatusCode.Accepted)
    }

    get("/jobs") {
        val state = call.request.queryParameters["state"]
            ?.let { runCatching { JobState.valueOf(it) }.getOrNull() }
        call.respondText(json.encodeToString(store.all(state)), APP_JSON)
    }

    get("/jobs/{id}") {
        val job = store.get(call.parameters["id"] ?: "") ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondText(json.encodeToString(job), APP_JSON)
    }

    delete("/jobs/{id}") {
        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val job = store.get(id) ?: return@delete call.respond(HttpStatusCode.NotFound)
        if (job.state == JobState.COMPLETED || job.state == JobState.FAILED || job.state == JobState.CANCELLED) {
            call.respond(HttpStatusCode.Conflict)
            return@delete
        }
        store.cancel(id)
        call.respond(HttpStatusCode.NoContent)
    }
}
