package com.ronjunevaldoz.graphyn

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val runtime = createGraphynServerRuntime()
    val registry = GraphynRunRegistry(runtime.executionEngine)
    // Compact (not pretty): SSE frame data must stay on a single line.
    val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    install(SSE)
    install(GraphynAuthPlugin)

    routing {
        get("/") {
            call.respondText("Graphyn server is running")
        }
        executionRoutes(runtime, registry, json)
    }
}
