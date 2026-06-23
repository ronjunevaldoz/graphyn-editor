package com.ronjunevaldoz.graphyn

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond

/**
 * Ktor plugin that enforces Bearer-token authentication when [GRAPHYN_API_KEY_ENV] is set.
 *
 * If the env var is absent the plugin is a no-op, so development servers need no token.
 * The health-check route `GET /` is always exempt so load-balancers can probe it freely.
 *
 * To protect a server: set `GRAPHYN_API_KEY=<secret>` in the environment before starting,
 * then pass `Authorization: Bearer <secret>` in every request header.
 */
const val GRAPHYN_API_KEY_ENV = "GRAPHYN_API_KEY"

val GraphynAuthPlugin = createApplicationPlugin("GraphynAuth") {
    val apiKey = System.getenv(GRAPHYN_API_KEY_ENV) ?: return@createApplicationPlugin

    onCall { call ->
        if (call.request.local.uri == "/" && call.request.local.method.value == "GET") return@onCall
        val bearer = call.request.headers[HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")?.trim()
        if (bearer != apiKey) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or missing API key")
        }
    }
}
