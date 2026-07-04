package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.GraphynEnvironment
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.core.store.InMemorySettingsStore
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerSdApiTest {
    @Test
    fun defaultsToLocalhost() {
        assertEquals("http://127.0.0.1:5000", resolveSdConnection(GraphynSettings()).baseUrl)
    }

    @Test
    fun apiChecksReadinessJobsCancelAndGenerate() = runTest {
        val transport = FakeServerSdTransport()
        transport.respond("GET", "/ping", 200, "pong".encodeToByteArray())
        transport.respond("GET", "/api/sd/status", 200, """{"busy":false}""".encodeToByteArray())
        transport.respond("GET", "/api/sd/jobs", 200, """[{"id":"job-1","state":"RUNNING","submittedAt":1}]""".encodeToByteArray())
        transport.respond("POST", "/api/sd/cancel/job-1", 202, """{"cancelled":true}""".encodeToByteArray())
        transport.respond("POST", "/api/sd/models/exists", 200, """{"missing":[]}""".encodeToByteArray())
        transport.respond("POST", "/api/sd/upload?ext=png", 200, """{"path":"/server/input.png"}""".encodeToByteArray())
        transport.respond("POST", "/api/sd/generate-ex", 200, byteArrayOf(1, 2, 3))
        val settings = InMemorySettingsStore(
            GraphynSettings(environments = listOf(GraphynEnvironment("default", mapOf(GraphynSettings.KEY_SD_URL to "http://worker")))),
        )
        val api = ServerSdApi(settings, transport)
        assertEquals("RUNNING", api.jobs().single().state)
        assertTrue(api.cancel("job-1"))
        transport.respond("GET", "/api/sd/jobs", 200, "[]".encodeToByteArray())
        val temp = File.createTempFile("graphyn-test", ".png").apply { writeText("x"); deleteOnExit() }
        val bytes = api.generateImage(SdGenerateImageRequest(prompt = "hello", initImagePath = temp.absolutePath))
        assertContentEquals(byteArrayOf(1, 2, 3), bytes)
        assertTrue(transport.calls.any { it == "GET /ping" })
        assertTrue(transport.calls.any { it == "POST /api/sd/cancel/job-1" })
        assertTrue(transport.calls.any { it == "POST /api/sd/upload?ext=png" })
        assertTrue(transport.calls.any { it == "POST /api/sd/generate-ex" })
    }
}

private class FakeServerSdTransport : ServerSdTransport {
    val calls = mutableListOf<String>()
    private val responses = mutableMapOf<String, ServerSdResponse>()
    fun respond(method: String, path: String, status: Int, body: ByteArray) {
        responses["$method $path"] = ServerSdResponse(status, body)
    }
    override suspend fun get(path: String, conn: SdConnection) = reply("GET", path)
    override suspend fun postJson(path: String, json: String, conn: SdConnection) = reply("POST", path)
    override suspend fun postBytes(path: String, bytes: ByteArray, conn: SdConnection) = reply("POST", path)
    override suspend fun delete(path: String, conn: SdConnection) = reply("DELETE", path)
    private fun reply(method: String, path: String): ServerSdResponse {
        calls += "$method $path"
        return responses["$method $path"] ?: ServerSdResponse(500, "missing $method $path".encodeToByteArray())
    }
}
