package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl

/**
 * [SdServerControl] backed by HTTP to server-sd. Resolves the server URL + API key from settings
 * per call (so it follows the active environment), like [HttpStableDiffusionBackend].
 */
class HttpSdServerControl(
    private val settingsStore: FileSettingsStore = FileSettingsStore(),
) : SdServerControl {
    private val api = ServerSdApi(settingsStore)

    override suspend fun ping(): Boolean = api.ping()
    override suspend fun status() = api.status()
    override suspend fun jobs() = api.jobs()
    override suspend fun cancel(jobId: String) = api.cancel(jobId)
    override suspend fun unload() = api.unload()
}
