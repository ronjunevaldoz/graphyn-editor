package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.serialization.toJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Publishes a [workflow] to a running `:server`'s [com.ronjunevaldoz.graphyn.WorkflowRoutes]
 * (`POST /workflows`), so it becomes inspectable via `GET /workflows/{id}` and loadable in the
 * graphyn-editor UI — CLI-run workflows (`workflow=<name>`) never touch this store on their own,
 * since `runWorkflowCli` builds and executes the [WorkflowDefinition] directly, bypassing the
 * server entirely.
 *
 * `server=<url>` (default `http://localhost:8080`, matching `:server`'s own hardcoded port) picks
 * which server to publish to.
 */
internal suspend fun publishWorkflow(workflow: WorkflowDefinition, serverUrl: String) {
    val client = HttpClient(CIO)
    client.use { client ->
        val response = client.post("$serverUrl/workflows") {
            contentType(ContentType.Application.Json)
            setBody(workflow.toJson())
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "Publish failed: ${response.status} $body" }
        println("Published '${workflow.name}' (id=${workflow.id}) to $serverUrl/workflows/${workflow.id}")
        println(body)
    }
}
