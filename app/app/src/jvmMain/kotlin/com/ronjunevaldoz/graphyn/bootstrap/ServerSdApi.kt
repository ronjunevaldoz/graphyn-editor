package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.editor.server.SdServerJobModel
import com.ronjunevaldoz.graphyn.editor.server.SdServerStatusModel
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val serverJson = Json { ignoreUnknownKeys = true }

internal class ServerSdApi(
    private val settingsStore: SettingsStore,
    private val transport: ServerSdTransport = HttpServerSdTransport(),
) {
    suspend fun ping(): Boolean = call("/ping") { path, conn -> transport.get(path, conn) }.ok
    suspend fun status(): SdServerStatusModel? = callModel("/api/sd/status")
    suspend fun jobs(): List<SdServerJobModel> = callList("/api/sd/jobs")
    suspend fun cancel(jobId: String): Boolean = call(if (jobId.isBlank()) "/api/sd/cancel" else "/api/sd/cancel/${jobId.escapePath()}") { path, conn ->
        transport.postJson(path, "{}", conn)
    }.ok
    suspend fun unload(): SdServerStatusModel? = callModel("/api/sd/unload", post = true)

    suspend fun generateImage(request: SdGenerateImageRequest): ByteArray {
        val conn = connection()
        ensureReady()
        val staged = stageLocalImages(request, conn)
        assertServerFilesExist(collectServerPaths(staged), conn)
        return postBytes("/api/sd/generate-ex", imageRequestToJson(staged), conn)
    }

    suspend fun generateVideo(request: SdGenerateVideoRequest): ByteArray {
        val conn = connection()
        ensureReady()
        val staged = stageLocalVideoImages(request, conn)
        assertServerFilesExist(collectServerPaths(staged), conn)
        return postBytes("/api/sd/generate-video", videoRequestToJson(staged), conn)
    }

    suspend fun ready(): Boolean =
        ping() && status()?.busy != true && jobs().none { it.state == "QUEUED" || it.state == "RUNNING" }

    suspend fun ensureReady() {
        check(ready()) { "server-sd is not ready; check /ping, /api/sd/status, and /api/sd/jobs" }
    }

    private suspend fun stageLocalImages(request: SdGenerateImageRequest, conn: SdConnection): SdGenerateImageRequest =
        request.copy(
            initImagePath = request.initImagePath?.let { uploadIfLocal(it, conn) },
            controlNet = request.controlNet?.let { cn ->
                cn.copy(
                    controlImage = cn.controlImage?.let { uploadIfLocal(it, conn) },
                    maskImage = cn.maskImage?.let { uploadIfLocal(it, conn) },
                )
            },
            // Previously unstaged: a local ref-image path (PhotoMaker/PuLID/Qwen-Image-Edit
            // conditioning) sent straight through to a remote server-sd was never found on its
            // filesystem — and rather than erroring, generation silently ignored the reference
            // entirely and produced an unconditioned image from the prompt alone.
            idCond = request.idCond?.let { ic -> ic.copy(refImages = ic.refImages.map { uploadIfLocal(it, conn) }) },
        )

    private suspend fun stageLocalVideoImages(request: SdGenerateVideoRequest, conn: SdConnection): SdGenerateVideoRequest =
        request.copy(initImagePath = request.initImagePath?.let { uploadIfLocal(it, conn) })

    private suspend fun uploadIfLocal(path: String, conn: SdConnection): String {
        val file = java.io.File(path)
        if (!file.isFile) return path
        val ext = file.extension.ifBlank { "png" }
        val resp = transport.postBytes("/api/sd/upload?ext=$ext", file.readBytes(), conn)
        check(resp.ok) { "server-sd upload responded ${resp.status}: ${resp.body.text()}" }
        return Regex("\"path\"\\s*:\\s*\"([^\"]+)\"").find(resp.body.text())?.groupValues?.get(1)
            ?: error("server-sd upload returned no path: ${resp.body.text()}")
    }

    private suspend fun assertServerFilesExist(paths: List<String>, conn: SdConnection) {
        if (paths.isEmpty()) return
        val body = paths.joinToString(",") { "\"${it.escapeJson()}\"" }
        val resp = transport.postJson("/api/sd/models/exists", """{"paths":[$body]}""", conn)
        if (!resp.ok) return
        val text = resp.body.text()
        val missing = Regex("\"([^\"]+)\"")
            .findAll(text.substringAfter("\"missing\"", "").substringAfter('[', "").substringBefore(']'))
            .map { it.groupValues[1] }.toList()
        require(missing.isEmpty()) { "Missing on server:\n" + missing.joinToString("\n") { "  $it" } }
    }

    private suspend fun callModel(path: String, post: Boolean = false): SdServerStatusModel? {
        val conn = connection()
        val resp = if (post) transport.postJson(path, "{}", conn) else transport.get(path, conn)
        return resp.body.text().takeIf { resp.ok }?.let { runCatching { serverJson.decodeFromString(SdServerStatusModel.serializer(), it) }.getOrNull() }
    }

    private suspend fun callList(path: String): List<SdServerJobModel> {
        val conn = connection()
        val resp = transport.get(path, conn)
        return resp.body.text().takeIf { resp.ok }?.let {
            runCatching { serverJson.decodeFromString(ListSerializer(SdServerJobModel.serializer()), it) }.getOrNull()
        }.orEmpty()
    }

    private suspend fun postBytes(path: String, body: String, conn: SdConnection): ByteArray {
        val resp = transport.postJson(path, body, conn)
        check(resp.ok) { "server-sd responded ${resp.status}: ${resp.body.text()}" }
        return resp.body
    }

    private suspend fun call(path: String, request: suspend (String, SdConnection) -> ServerSdResponse): ServerSdResponse {
        val conn = connection()
        return runCatching { request(path, conn) }.getOrElse { ServerSdResponse(599, it.message.orEmpty().encodeToByteArray()) }
    }

    private suspend fun connection(): SdConnection = resolveSdConnection(settingsStore.load())
}

private val ServerSdResponse.ok get() = status in 200..299
private fun ByteArray.text() = toString(Charsets.UTF_8)
private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")
private fun String.escapePath() = replace("/", "%2F")
