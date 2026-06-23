package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
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
import kotlinx.serialization.json.Json

private val JSON_CT = ContentType.Application.Json

/**
 * CRUD routes for persisted workflows:
 *
 * - `GET  /workflows`        → `[WorkflowMeta]` list, newest first
 * - `GET  /workflows/{id}`   → full [WorkflowDefinition] (404 when not found)
 * - `POST /workflows`        → save/upsert; returns updated [WorkflowMeta] (201)
 * - `DELETE /workflows/{id}` → deletes workflow and all history; 204 No Content
 */
fun Route.workflowRoutes(store: WorkflowStore, json: Json) {
    get("/workflows") {
        call.respondText(json.encodeToString<List<WorkflowMeta>>(store.list()), JSON_CT)
    }

    get("/workflows/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val wf = store.load(id) ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondText(DefaultWorkflowJsonCodec.encodeToString(wf), JSON_CT)
    }

    post("/workflows") {
        val wf = DefaultWorkflowJsonCodec.decodeFromString(call.receiveText())
        val meta = store.save(wf)
        call.respondText(json.encodeToString(meta), JSON_CT, HttpStatusCode.Created)
    }

    delete("/workflows/{id}") {
        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        store.delete(id)
        call.respond(HttpStatusCode.NoContent)
    }
}
