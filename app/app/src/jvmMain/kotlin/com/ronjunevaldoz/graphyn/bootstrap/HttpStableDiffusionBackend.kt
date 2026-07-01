package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.ArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.FileArtifactHistory
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdImageResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdVideoResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionBackend
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
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
    private val baseUrl: String = System.getenv("GRAPHYN_SD_SERVER_URL")
        ?: "https://ron-local-home.duckdns.org/stablediffusion",
    private val artifactsDir: File = defaultArtifactsDir(),
    private val history: ArtifactHistory = FileArtifactHistory(),
) : StableDiffusionBackend {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 600_000
        }
        // Authenticate to a publicly-exposed server-sd when a key is configured (no-op otherwise).
        System.getenv("GRAPHYN_SD_API_KEY")?.ifBlank { null }?.let { key ->
            install(DefaultRequest) { header("Authorization", "Bearer $key") }
        }
    }

    override fun generateImage(args: List<String>): SdImageResult {
        val file = runBlocking {
            val staged = stageLocalImages(args)
            assertServerFilesExist(staged)
            val start = System.currentTimeMillis()
            val bytes = postJson("$baseUrl/api/sd/generate-ex", argsToJson(staged))
            saveArtifact(bytes, "png").also {
                history.record(buildArtifactRecord(it, ArtifactKind.Image, args, System.currentTimeMillis() - start, "sd.txt2img"))
            }
        }
        return SdImageResult(imagePaths = listOf(file.absolutePath))
    }

    override fun generateVideo(args: List<String>): SdVideoResult {
        val file = runBlocking {
            val staged = stageLocalImages(args)
            assertServerFilesExist(staged)
            val start = System.currentTimeMillis()
            val bytes = postJson("$baseUrl/api/sd/generate-video", videoArgsToJson(staged))
            saveArtifact(bytes, "mp4").also {
                history.record(buildArtifactRecord(it, ArtifactKind.Video, args, System.currentTimeMillis() - start, "sd.img2vid"))
            }
        }
        return SdVideoResult(framePaths = listOf(file.absolutePath))
    }

    // Pre-flight: fail fast with a clear list when a model/LoRA/input file is missing on the server,
    // instead of after a multi-minute model load (or a native OOM crash).
    private suspend fun assertServerFilesExist(args: List<String>) {
        val paths = collectServerPaths(args)
        if (paths.isEmpty()) return
        val body = paths.joinToString(",") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
        val resp = client.post("$baseUrl/api/sd/models/exists") {
            contentType(ContentType.Application.Json); setBody("""{"paths":[$body]}""")
        }
        if (!resp.status.isSuccess()) return // older server without the endpoint — skip the check
        val text = resp.bodyAsBytes().toString(Charsets.UTF_8)
        val missing = Regex("\"([^\"]+)\"")
            .findAll(text.substringAfter("\"missing\"", "").substringAfter('[', "").substringBefore(']'))
            .map { it.groupValues[1] }.toList()
        if (missing.isNotEmpty()) {
            error("Missing on server (check the model paths on the sd.* nodes):\n" + missing.joinToString("\n") { "  $it" })
        }
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
