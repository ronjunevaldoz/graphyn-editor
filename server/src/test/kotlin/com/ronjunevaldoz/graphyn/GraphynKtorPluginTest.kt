package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for [Graphyn] Ktor plugin configuration — route prefix, auth opt-out, extra plugins.
 * Core execution correctness is covered in [ApplicationTest].
 */
class GraphynKtorPluginTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun simpleWorkflow() = WorkflowDefinition(
        id = "plugin-test",
        name = "Plugin Test",
        nodes = listOf(
            NodeRef(id = "parse", type = "json.parse",
                config = mapOf("text" to WorkflowValue.StringValue("""{"x":1}"""))),
        ),
        connections = emptyList(),
    )

    // ── routePrefix ────────────────────────────────────────────────────────────

    @Test
    fun defaultConfigMountsRoutesAtRoot() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val response = client.post("/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun routePrefixMovesAllRoutesUnderPrefix() = testApplication {
        application {
            install(Graphyn) {
                routePrefix = "/api/graphyn"
                requireApiKey = false
            }
            routing { get("/") { call.respondText("ok") } }
        }
        // Routes under the prefix work.
        val prefixed = client.post("/api/graphyn/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.OK, prefixed.status)

        // Bare /execute is gone.
        val bare = client.post("/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.NotFound, bare.status)
    }

    // ── requireApiKey = false ─────────────────────────────────────────────────

    @Test
    fun requireApiKeyFalseAllowsUnauthenticatedRequests() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val response = client.post("/validate") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        // No auth header needed — should succeed (empty error list = 200).
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ── apiKey (explicit key, no env var needed) ──────────────────────────────

    @Test
    fun apiKeyEnforcedWhenSet() = testApplication {
        application {
            install(Graphyn) {
                requireApiKey = true
                apiKey = "test-secret"
            }
            routing { get("/") { call.respondText("ok") } }
        }
        // No token → 401.
        val noToken = client.post("/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.Unauthorized, noToken.status)

        // Wrong token → 401.
        val wrongToken = client.post("/execute") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer wrong")
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.Unauthorized, wrongToken.status)

        // Correct token → 200.
        val goodToken = client.post("/execute") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer test-secret")
            setBody(DefaultWorkflowJsonCodec.encodeToString(simpleWorkflow()))
        }
        assertEquals(HttpStatusCode.OK, goodToken.status)
        val result = json.decodeFromString(WorkflowExecutionResult.serializer(), goodToken.bodyAsText())
        assertTrue(result.isFullSuccess)
    }

    @Test
    fun healthCheckExemptFromAuth() = testApplication {
        application {
            install(Graphyn) { requireApiKey = true; apiKey = "secret" }
            routing { get("/") { call.respondText("ok") } }
        }
        // GET / must pass even without a token (load-balancer probe exemption).
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ── extraPlugins ──────────────────────────────────────────────────────────

    @Test
    fun extraPluginNodesAreExecutable() = testApplication {
        val echoPlugin = object : GraphynPlugin {
            override val metadata = GraphynPluginMetadata("test.echo", "Echo Plugin", "0.0.1")
            override fun register(registrar: GraphynPluginRegistrar) {
                registrar.registerNodeSpec(NodeSpec(
                    type = "test.echo",
                    label = "Echo",
                    inputs = listOf(PortSpec("value", WorkflowType.StringType)),
                    outputs = listOf(PortSpec("value", WorkflowType.StringType)),
                ))
                registrar.registerExecutor("test.echo") { inputs ->
                    mapOf("value" to (inputs["value"] ?: WorkflowValue.StringValue("")))
                }
            }
        }

        application {
            install(Graphyn) {
                requireApiKey = false
                plugins(echoPlugin)
            }
        }

        val workflow = WorkflowDefinition(
            id = "echo-test", name = "Echo Test",
            nodes = listOf(
                NodeRef("n1", "test.echo",
                    config = mapOf("value" to WorkflowValue.StringValue("hello"))),
            ),
            connections = emptyList(),
        )
        val response = client.post("/execute") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(workflow))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = json.decodeFromString(WorkflowExecutionResult.serializer(), response.bodyAsText())
        assertTrue(result.isFullSuccess)
        assertEquals(WorkflowValue.StringValue("hello"), result.nodeOutputsByNodeId["n1"]?.get("value"))
    }

    @Test
    fun unknownNodeTypeValidationFailsBeforeExecution() = testApplication {
        application { install(Graphyn) { requireApiKey = false } }
        val workflow = WorkflowDefinition(
            id = "bad", name = "Bad",
            nodes = listOf(NodeRef("x", "test.echo")),   // not installed
            connections = emptyList(),
        )
        val response = client.post("/validate") {
            contentType(ContentType.Application.Json)
            setBody(DefaultWorkflowJsonCodec.encodeToString(workflow))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val errors = json.decodeFromString<List<com.ronjunevaldoz.graphyn.core.model.ValidationError>>(response.bodyAsText())
        assertTrue(errors.any { it.code == "unknown_node_type" })
    }
}
