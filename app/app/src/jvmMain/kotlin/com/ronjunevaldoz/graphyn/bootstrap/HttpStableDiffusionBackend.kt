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
import java.io.File

/**
 * Forwards generation to a running `server-sd` instance over HTTP.
 * Translates the executor's CLI-flag list into a JSON body for `/api/sd/generate-ex`
 * (images) or `/api/sd/generate-video` (Wan2.2 video), then persists the returned bytes.
 *
 * Outputs are written to a durable artifacts directory (not the OS temp dir) so generated
 * media survives app restarts and can be fed back in as `init_image` for another run.
 * Configure the server URL via `GRAPHYN_SD_SERVER_URL` and the output dir via
 * `GRAPHYN_ARTIFACTS_DIR` (default `~/.graphyn/artifacts`).
 */
class HttpStableDiffusionBackend(
    private val baseUrl: String = System.getenv("GRAPHYN_SD_SERVER_URL") ?: "http://192.168.254.104:8082",
    private val artifactsDir: File = defaultArtifactsDir(),
) : StableDiffusionBackend {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 600_000
        }
    }

    override fun generateImage(args: List<String>): SdImageResult {
        val bytes = runBlocking {
            postJson("$baseUrl/api/sd/generate-ex", argsToJson(stageLocalImages(args)))
        }
        return SdImageResult(imagePaths = listOf(saveArtifact(bytes, "png").absolutePath))
    }

    override fun generateVideo(args: List<String>): SdVideoResult {
        val bytes = runBlocking {
            postJson("$baseUrl/api/sd/generate-video", videoArgsToJson(stageLocalImages(args)))
        }
        return SdVideoResult(framePaths = listOf(saveArtifact(bytes, "mp4").absolutePath))
    }

    private fun saveArtifact(bytes: ByteArray, ext: String): File {
        artifactsDir.mkdirs()
        val file = File(artifactsDir, "graphyn-sd-${System.currentTimeMillis()}-${counter.incrementAndGet()}.$ext")
        file.writeBytes(bytes)
        return file
    }

    private companion object {
        val counter = java.util.concurrent.atomic.AtomicLong(0)

        fun defaultArtifactsDir(): File =
            System.getenv("GRAPHYN_ARTIFACTS_DIR")?.ifBlank { null }?.let(::File)
                ?: File(System.getProperty("user.home"), ".graphyn/artifacts")
    }

    // Path-based image inputs are resolved on the server's filesystem, so any local file must be
    // uploaded first and its flag value rewritten to the returned server path.
    private suspend fun stageLocalImages(args: List<String>): List<String> {
        val imageFlags = setOf("--init-img", "--control-image", "--mask")
        val out = args.toMutableList()
        var i = 0
        while (i < out.size) {
            if (out[i] in imageFlags && i + 1 < out.size) {
                out[i + 1] = uploadIfLocal(out[i + 1])
                i += 2
            } else i++
        }
        return out
    }

    private suspend fun uploadIfLocal(path: String): String {
        val file = File(path)
        if (!file.isFile) return path // already a server-side path, or nothing to upload
        val ext = file.extension.ifBlank { "png" }
        val response = client.post("$baseUrl/api/sd/upload?ext=$ext") { setBody(file.readBytes()) }
        check(response.status.isSuccess()) {
            "server-sd upload responded ${response.status}: ${response.bodyAsBytes().toString(Charsets.UTF_8)}"
        }
        val body = response.bodyAsBytes().toString(Charsets.UTF_8)
        return Regex("\"path\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: error("server-sd upload returned no path: $body")
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
