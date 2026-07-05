package com.ronjunevaldoz.graphyn

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Graphyn)

    routing {
        get("/") { call.respondText("Graphyn server is running") }
        // Browsable, "Try it out"-capable API explorer — not part of the embeddable Graphyn
        // plugin itself, so consumers embedding install(Graphyn) in their own app don't get an
        // unrequested Swagger UI; this is specific to running :server standalone.
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}
