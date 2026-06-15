package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import kotlinx.serialization.json.Json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val runtime = createGraphynServerRuntime()
    val json = Json {
        encodeDefaults = false
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    routing {
        get("/") {
            call.respondText("Graphyn server is running")
        }
        post("/execute") {
            val workflow = DefaultWorkflowJsonCodec.decodeFromString(call.receiveText())
            val result = runtime.executionEngine.execute(workflow)
            call.respondText(
                json.encodeToString(
                    WorkflowExecutionResult.serializer(),
                    result,
                ),
            )
        }
    }
}
