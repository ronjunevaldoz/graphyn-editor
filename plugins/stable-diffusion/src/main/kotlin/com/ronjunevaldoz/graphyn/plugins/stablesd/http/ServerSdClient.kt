package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdServerConfig
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val serverJson = Json { ignoreUnknownKeys = true }

/**
 * Talks HTTP to a running `server-sd` instance: readiness (`ping`/`status`/`jobs`), lifecycle
 * (`cancel`/`unload`), and generation (`generateImage`/`generateVideo`, byte results — callers
 * decide how/where to persist them). The server URL + API key are resolved fresh **per call**
 * from [settingsStore] (then env, then a `127.0.0.1:5000` default), so a credential change
 * applies on the next call without a restart. [SdGenerateImageRequest.server] /
 * [SdGenerateVideoRequest.server] (populated by wiring an `sd.server` node) override that
 * resolution for a single generation, so one workflow can target multiple deployments.
 */
class ServerSdClient internal constructor(
    private val settingsStore: SettingsStore,
    private val transport: ServerSdTransport,
) {
    constructor(settingsStore: SettingsStore) : this(settingsStore, HttpServerSdTransport())


    suspend fun ping(conn: SdConnection? = null): Boolean = call(conn ?: connection(), "/ping") { path, c -> transport.get(path, c) }.ok
    suspend fun status(conn: SdConnection? = null): SdServerStatus? = callModel(conn ?: connection(), "/api/sd/status")
    suspend fun jobs(conn: SdConnection? = null): List<SdServerJob> = callList(conn ?: connection(), "/api/sd/jobs")
    suspend fun cancel(jobId: String, conn: SdConnection? = null): Boolean =
        call(conn ?: connection(), if (jobId.isBlank()) "/api/sd/cancel" else "/api/sd/cancel/${jobId.escapePath()}") { path, c ->
            transport.postJson(path, "{}", c)
        }.ok
    suspend fun unload(conn: SdConnection? = null): SdServerStatus? = callModel(conn ?: connection(), "/api/sd/unload", post = true)

    suspend fun load(request: LoadModelRequest, conn: SdConnection? = null): SdServerStatus? {
        val resp = transport.postJson("/api/sd/load", serverJson.encodeToString(LoadModelRequest.serializer(), request), conn ?: connection())
        return resp.body.text().takeIf { resp.ok }?.let { runCatching { serverJson.decodeFromString(SdServerStatus.serializer(), it) }.getOrNull() }
    }

    suspend fun generateImage(request: SdGenerateImageRequest): ByteArray {
        val conn = connection(request.server)
        ensureReady(conn)
        val staged = stageLocalImages(request, conn)
        assertServerFilesExist(collectServerPaths(staged), conn)
        return postBytes("/api/sd/generate-ex", imageRequestToJson(staged), conn)
    }

    suspend fun generateVideo(request: SdGenerateVideoRequest): ByteArray {
        val conn = connection(request.server)
        ensureReady(conn)
        val staged = stageLocalVideoImages(request, conn)
        assertServerFilesExist(collectServerPaths(staged), conn)
        return postBytes("/api/sd/generate-video", videoRequestToJson(staged), conn)
    }

    suspend fun ready(conn: SdConnection? = null): Boolean {
        val c = conn ?: connection()
        return ping(c) && status(c)?.busy != true && jobs(c).none { it.state == "QUEUED" || it.state == "RUNNING" }
    }

    suspend fun ensureReady(conn: SdConnection? = null) {
        val c = conn ?: connection()
        check(ready(c)) { "server-sd is not ready; check /ping, /api/sd/status, and /api/sd/jobs" }
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
            // A local ref-image path (PhotoMaker/PuLID/Qwen-Image-Edit conditioning) sent straight
            // through to a remote server-sd is never found on its filesystem — rather than erroring,
            // generation silently ignores the reference and produces an unconditioned image.
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

    private suspend fun callModel(conn: SdConnection, path: String, post: Boolean = false): SdServerStatus? {
        val resp = if (post) transport.postJson(path, "{}", conn) else transport.get(path, conn)
        return resp.body.text().takeIf { resp.ok }?.let { runCatching { serverJson.decodeFromString(SdServerStatus.serializer(), it) }.getOrNull() }
    }

    private suspend fun callList(conn: SdConnection, path: String): List<SdServerJob> {
        val resp = transport.get(path, conn)
        return resp.body.text().takeIf { resp.ok }?.let {
            runCatching { serverJson.decodeFromString(ListSerializer(SdServerJob.serializer()), it) }.getOrNull()
        }.orEmpty()
    }

    private suspend fun postBytes(path: String, body: String, conn: SdConnection): ByteArray {
        val resp = transport.postJson(path, body, conn)
        check(resp.ok) { "server-sd responded ${resp.status}: ${resp.body.text()}" }
        return resp.body
    }

    private suspend fun call(conn: SdConnection, path: String, request: suspend (String, SdConnection) -> ServerSdResponse): ServerSdResponse {
        return runCatching { request(path, conn) }.getOrElse { ServerSdResponse(599, it.message.orEmpty().encodeToByteArray()) }
    }

    /**
     * Resolves the effective connection: [override] fields win when non-blank, otherwise fall back
     * to [settingsStore] → env var → default (see [resolveSdConnection]). Passing no override keeps
     * every call site's prior behavior (app-wide server).
     */
    private suspend fun connection(override: SdServerConfig? = null): SdConnection {
        val base = resolveSdConnection(settingsStore.load())
        return SdConnection(
            baseUrl = override?.baseUrl?.ifBlank { null }?.trimEnd('/') ?: base.baseUrl,
            apiKey = override?.apiKey?.ifBlank { null } ?: base.apiKey,
        )
    }
}

private val ServerSdResponse.ok get() = status in 200..299
private fun ByteArray.text() = toString(Charsets.UTF_8)
private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")
private fun String.escapePath() = replace("/", "%2F")
