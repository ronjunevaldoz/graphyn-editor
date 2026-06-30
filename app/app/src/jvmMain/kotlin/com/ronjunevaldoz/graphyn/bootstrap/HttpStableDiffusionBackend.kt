package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.plugins.stablesd.SdImageResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdVideoResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionBackend
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import java.nio.file.Files

/**
 * Forwards generation to a running `server-sd` instance over HTTP.
 * Translates the executor's CLI-flag list into a JSON body for `/api/sd/generate-ex`,
 * writes the returned PNG bytes to a temp file, and returns its path.
 *
 * Configure the server URL via the `GRAPHYN_SD_SERVER_URL` env var.
 */
class HttpStableDiffusionBackend(
    private val baseUrl: String = System.getenv("GRAPHYN_SD_SERVER_URL") ?: "http://192.168.254.104:8082",
) : StableDiffusionBackend {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 300_000
        }
    }

    override fun generateImage(args: List<String>): SdImageResult {
        val bytes = runBlocking { postJson("$baseUrl/api/sd/generate-ex", argsToJson(args)) }
        val tmp = Files.createTempFile("graphyn-sd-", ".png").toFile()
        tmp.writeBytes(bytes)
        return SdImageResult(imagePaths = listOf(tmp.absolutePath))
    }

    override fun generateVideo(args: List<String>): SdVideoResult {
        throw UnsupportedOperationException("Video generation via HTTP backend is not yet implemented")
    }

    private suspend fun postJson(url: String, json: String): ByteArray {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(json)
        }
        check(response.status.isSuccess()) {
            "server-sd responded ${response.status}: ${response.bodyAsBytes().toString(Charsets.UTF_8)}"
        }
        return response.bodyAsBytes()
    }
}
