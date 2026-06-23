package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.ExecutionStreamMessage
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RunAccepted(val runId: String)

private val APP_JSON = ContentType.Application.Json

/**
 * Wires the workflow execution API onto [Route]:
 *
 * - `POST /validate`           → `[ValidationError]` (200, empty list means valid)
 * - `POST /execute`            → [WorkflowExecutionResult] (synchronous, whole run)
 * - `POST /executions`         → `{ runId }` (202) after validation, run starts in the background
 * - `GET  /executions/{id}/events` (SSE) → [ExecutionStreamMessage] frames until a terminal frame
 *
 * Requires the `SSE` plugin to be installed on the application.
 */
fun Route.executionRoutes(
    runtime: GraphynServerRuntime,
    registry: GraphynRunRegistry,
    json: Json,
) {
    post("/validate") {
        val workflow = DefaultWorkflowJsonCodec.decodeFromString(call.receiveText())
        val errors = runtime.validator.validate(workflow)
        call.respondText(json.encodeToString<List<ValidationError>>(errors), APP_JSON)
    }

    post("/execute") {
        val workflow = DefaultWorkflowJsonCodec.decodeFromString(call.receiveText())
        val result = runtime.executionEngine.execute(workflow)
        call.respondText(json.encodeToString(result), APP_JSON)
    }

    post("/executions") {
        if (!registry.canAcceptRun) {
            call.respondText("Server is at capacity — try again later", status = HttpStatusCode.ServiceUnavailable)
            return@post
        }
        val workflow = DefaultWorkflowJsonCodec.decodeFromString(call.receiveText())
        val errors = runtime.validator.validate(workflow)
        if (errors.isNotEmpty()) {
            call.respondText(json.encodeToString<List<ValidationError>>(errors), APP_JSON, HttpStatusCode.BadRequest)
            return@post
        }
        val runId = registry.start(workflow)
        call.respondText(json.encodeToString(RunAccepted(runId)), APP_JSON, HttpStatusCode.Accepted)
    }

    sse("/executions/{id}/events") {
        val runId = call.parameters["id"]
        val flow = runId?.let { registry.messages(it) }
        if (flow == null) {
            send(ServerSentEvent(data = json.encodeToString<ExecutionStreamMessage>(
                ExecutionStreamMessage.Failed("Unknown run id")), event = "failed"))
            return@sse
        }
        // Emit each frame, including the terminal one, then stop.
        flow.transformWhile { msg ->
            emit(msg)
            msg is ExecutionStreamMessage.Event
        }.collect { msg ->
            send(ServerSentEvent(data = json.encodeToString<ExecutionStreamMessage>(msg), event = msg.eventName()))
        }
    }
}

private fun ExecutionStreamMessage.eventName(): String = when (this) {
    is ExecutionStreamMessage.Event -> "event"
    is ExecutionStreamMessage.Completed -> "completed"
    is ExecutionStreamMessage.Failed -> "failed"
}
