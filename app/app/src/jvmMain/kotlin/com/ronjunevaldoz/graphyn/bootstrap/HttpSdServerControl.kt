package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl
import com.ronjunevaldoz.graphyn.editor.server.SdServerStatusModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * [SdServerControl] backed by HTTP to server-sd. Resolves the server URL + API key from settings
 * per call (so it follows the active environment), like [HttpStableDiffusionBackend].
 */
class HttpSdServerControl(
    private val settingsStore: FileSettingsStore = FileSettingsStore(),
    private val client: HttpClient = HttpClient(CIO),
) : SdServerControl {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun status(): SdServerStatusModel? = request { c ->
        client.get("${c.baseUrl}/api/sd/status") { authWith(c) }
    }

    override suspend fun unload(): SdServerStatusModel? = request { c ->
        client.post("${c.baseUrl}/api/sd/unload") { authWith(c) }
    }

    private suspend fun request(
        call: suspend (SdConnection) -> io.ktor.client.statement.HttpResponse,
    ): SdServerStatusModel? = runCatching {
        val resp = call(resolveSdConnection(settingsStore.read()))
        if (!resp.status.isSuccess()) return null
        json.decodeFromString<SdServerStatusModel>(resp.bodyAsText())
    }.getOrNull()
}
