package com.ronjunevaldoz.graphyn

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond

/**
 * Ktor plugin that enforces Bearer-token authentication.
 *
 * Resolution order for the key: [GraphynAuthConfig.apiKey] → `GRAPHYN_API_KEY` env var → no-op.
 * If neither is set the plugin is a no-op, so development servers need no token.
 * The health-check route `GET /` is always exempt so load-balancers can probe it freely.
 *
 * To protect a production server set `GRAPHYN_API_KEY=<secret>` in the environment, or supply
 * the key explicitly via [GraphynKtorConfig.apiKey] when embedding with `install(Graphyn)`.
 */
const val GRAPHYN_API_KEY_ENV = "GRAPHYN_API_KEY"

class GraphynAuthConfig {
    /** Explicit key. `null` falls back to the [GRAPHYN_API_KEY_ENV] env var. */
    var apiKey: String? = null
}

val GraphynAuthPlugin = createApplicationPlugin("GraphynAuth", ::GraphynAuthConfig) {
    val key = pluginConfig.apiKey ?: System.getenv(GRAPHYN_API_KEY_ENV) ?: return@createApplicationPlugin

    onCall { call ->
        if (call.request.local.uri == "/" && call.request.local.method.value == "GET") return@onCall
        val bearer = call.request.headers[HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")?.trim()
        if (bearer != key) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or missing API key")
        }
    }
}
