package com.ronjunevaldoz.graphyn

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.*

/** Tests for [GraphynAuthPlugin] in isolation — no workflow execution needed. */
class GraphynAuthPluginTest {

    private fun appWithKey(key: String?) = fun Application.() {
        install(GraphynAuthPlugin) { apiKey = key }
        routing {
            get("/") { call.respondText("home") }
            get("/protected") { call.respondText("secret") }
        }
    }

    @Test
    fun noKeyConfiguredAllowsAllRequests() = testApplication {
        application(appWithKey(null))   // null = no env var either in test = no-op
        assertEquals(HttpStatusCode.OK, client.get("/protected").status)
    }

    @Test
    fun keyConfiguredBlocksRequestsWithoutToken() = testApplication {
        application(appWithKey("my-key"))
        val response = client.get("/protected")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun keyConfiguredBlocksRequestsWithWrongToken() = testApplication {
        application(appWithKey("my-key"))
        val response = client.get("/protected") {
            header(HttpHeaders.Authorization, "Bearer wrong-key")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun keyConfiguredAllowsRequestsWithCorrectToken() = testApplication {
        application(appWithKey("my-key"))
        val response = client.get("/protected") {
            header(HttpHeaders.Authorization, "Bearer my-key")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("secret", response.bodyAsText())
    }

    @Test
    fun healthCheckRootGetIsAlwaysExempt() = testApplication {
        application(appWithKey("my-key"))
        // GET / must pass even with no token.
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("home", response.bodyAsText())
    }

    @Test
    fun nonGetRootIsNotExempt() = testApplication {
        application {
            install(GraphynAuthPlugin) { apiKey = "my-key" }
            routing {
                get("/") { call.respondText("home") }
                post("/") { call.respondText("post-home") }
                get("/protected") { call.respondText("secret") }
            }
        }
        // POST / is not the health-check exemption — must require token.
        val response = client.post("/")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun bearerPrefixWithExtraWhitespaceIsStripped() = testApplication {
        application(appWithKey("my-key"))
        val response = client.get("/protected") {
            header(HttpHeaders.Authorization, "Bearer   my-key")
        }
        // Extra whitespace is trimmed — should still authenticate.
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
