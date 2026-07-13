package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Runs a *stored* workflow as a parameterized API: callers supply only the inputs they want to
 * vary, keyed by node id then port name. This turns any saved workflow into an endpoint without
 * the caller re-sending (or even knowing) the full graph.
 *
 * @param overrides node id → (port name → value) merged over each node's config before running.
 * @param async true → return `{ runId }` (202) and stream via `GET …/executions/{id}/events`;
 *              false (default) → run synchronously and return the [WorkflowExecutionResult].
 */
@Serializable
data class RunRequest(
    val overrides: Map<String, Map<String, WorkflowValue>> = emptyMap(),
    val async: Boolean = false,
)

@Serializable
private data class AsyncRunStarted(val runId: String)

/** Returns a copy of [workflow] with [overrides] merged onto matching nodes' config (override wins). */
fun applyOverrides(
    workflow: WorkflowDefinition,
    overrides: Map<String, Map<String, WorkflowValue>>,
): WorkflowDefinition {
    if (overrides.isEmpty()) return workflow
    return workflow.copy(
        nodes = workflow.nodes.map { node ->
            overrides[node.id]?.let { node.copy(config = node.config + it) } ?: node
        },
    )
}

/**
 * `POST /workflows/{id}/run` — load a stored workflow, apply input overrides, validate, and execute.
 * 404 when the id is unknown; 400 with `[ValidationError]` when the merged graph is invalid.
 */
fun Route.workflowRunRoute(
    store: WorkflowStore,
    runtime: GraphynServerRuntime,
    registry: GraphynRunRegistry,
    json: Json,
) {
    post("/workflows/{id}/run") {
        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val stored = store.load(id) ?: return@post call.respond(HttpStatusCode.NotFound)

        val body = call.receiveText()
        val request = if (body.isBlank()) RunRequest() else json.decodeFromString<RunRequest>(body)
        val workflow = applyOverrides(stored, request.overrides)

        val errors = runtime.validator.validate(workflow)
        if (errors.isNotEmpty()) {
            return@post call.respondText(
                json.encodeToString<List<ValidationError>>(errors),
                ContentType.Application.Json, HttpStatusCode.BadRequest,
            )
        }

        if (request.async) {
            if (!registry.canAcceptRun) {
                return@post call.respondText("Server is at capacity — try again later", status = HttpStatusCode.ServiceUnavailable)
            }
            val runId = registry.start(workflow)
            call.respondText(json.encodeToString(AsyncRunStarted(runId)), ContentType.Application.Json, HttpStatusCode.Accepted)
        } else {
            val result = runtime.executionEngine.execute(workflow)
            call.respondText(json.encodeToString(result), ContentType.Application.Json)
        }
    }
}
