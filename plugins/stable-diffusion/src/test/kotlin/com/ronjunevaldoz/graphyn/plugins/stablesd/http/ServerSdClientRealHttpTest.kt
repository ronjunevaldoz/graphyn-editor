package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import com.ronjunevaldoz.graphyn.core.store.GraphynEnvironment
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.core.store.InMemorySettingsStore
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdServerConfig
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises [ServerSdClient] over **real loopback HTTP** (the actual [HttpServerSdTransport]/CIO
 * client, not [FakeServerSdTransport]) against two independent local servers standing in for two
 * `server-sd` deployments — proving an `sd.server` override actually redirects a generation call
 * to a different host, the mechanism that lets one workflow mix e.g. a local box and a Modal
 * deployment, rather than just asserting on in-memory plumbing.
 */
class ServerSdClientRealHttpTest {
    private val servers = mutableListOf<HttpServer>()

    @AfterTest
    fun tearDown() = servers.forEach { it.stop(0) }

    private fun startFakeSdServer(): Pair<String, ConcurrentLinkedQueue<String>> {
        val received = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun handle(path: String, status: Int, body: String) {
            server.createContext(path) { exchange ->
                received += "${exchange.requestMethod} $path auth=${exchange.requestHeaders.getFirst("Authorization")}"
                val bytes = body.encodeToByteArray()
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        handle("/ping", 200, "pong")
        handle("/api/sd/status", 200, """{"busy":false}""")
        handle("/api/sd/jobs", 200, "[]")
        handle("/api/sd/generate-ex", 200, "ok")
        server.start()
        servers += server
        return "http://127.0.0.1:${server.address.port}" to received
    }

    @Test
    fun generateImageHitsTheAppWideServerWhenNoOverrideIsWired() = runTest {
        val (serverAUrl, serverAHits) = startFakeSdServer()
        val (_, serverBHits) = startFakeSdServer()
        val settings = InMemorySettingsStore(
            GraphynSettings(environments = listOf(GraphynEnvironment("default", mapOf(GraphynSettings.KEY_SD_URL to serverAUrl)))),
        )
        val client = ServerSdClient(settings)

        client.generateImage(SdGenerateImageRequest(prompt = "hello"))

        assertTrue(serverAHits.any { it.startsWith("POST /api/sd/generate-ex") })
        assertTrue(serverBHits.isEmpty(), "unwired server.node must not receive traffic")
    }

    @Test
    fun sdServerNodeOverrideRedirectsGenerationToADifferentDeployment() = runTest {
        val (serverAUrl, serverAHits) = startFakeSdServer()
        val (serverBUrl, serverBHits) = startFakeSdServer()
        val settings = InMemorySettingsStore(
            GraphynSettings(environments = listOf(GraphynEnvironment("default", mapOf(GraphynSettings.KEY_SD_URL to serverAUrl)))),
        )
        val client = ServerSdClient(settings)

        // Simulates an sd.server node (base_url=serverBUrl, api_key="modal-key") wired into this
        // generation node's `server` port — extractServerConfig() produces exactly this shape.
        client.generateImage(SdGenerateImageRequest(prompt = "hello", server = SdServerConfig(baseUrl = serverBUrl, apiKey = "modal-key")))

        assertTrue(serverAHits.isEmpty(), "app-wide server must not receive traffic once overridden")
        assertTrue(serverBHits.any { it.startsWith("POST /api/sd/generate-ex") && it.contains("auth=Bearer modal-key") })
        // Readiness checks (ping/status/jobs) must also target the override, not the app-wide server.
        assertEquals(1, serverBHits.count { it.startsWith("GET /ping") })
        assertEquals(1, serverBHits.count { it.startsWith("GET /api/sd/status") })
        assertEquals(1, serverBHits.count { it.startsWith("GET /api/sd/jobs") })
    }
}
