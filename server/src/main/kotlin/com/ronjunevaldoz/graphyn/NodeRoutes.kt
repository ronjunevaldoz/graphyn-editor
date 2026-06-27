package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistry
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.json.Json

/**
 * Read-only catalog of all registered node specs:
 *
 * - `GET /nodes`        → `[NodeSpec]` list of every registered node type
 * - `GET /nodes/{type}` → single [NodeSpec] (404 when the type is not registered)
 */
fun Route.nodeRoutes(plugins: GraphynPluginRegistry, json: Json) {
    val ct = ContentType.Application.Json

    get("/nodes") {
        call.respondText(json.encodeToString<List<NodeSpec>>(plugins.nodeSpecs.all()), ct)
    }

    get("/nodes/{type}") {
        val type = call.parameters["type"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val spec = plugins.nodeSpecs.resolve(type) ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondText(json.encodeToString<NodeSpec>(spec), ct)
    }
}
