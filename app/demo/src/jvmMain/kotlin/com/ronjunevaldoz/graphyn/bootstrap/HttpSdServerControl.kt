package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.editor.server.SdGpu
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl
import com.ronjunevaldoz.graphyn.editor.server.SdServerJobModel
import com.ronjunevaldoz.graphyn.editor.server.SdServerStatusModel
import com.ronjunevaldoz.graphyn.plugins.stablesd.http.ServerSdClient
import com.ronjunevaldoz.graphyn.plugins.stablesd.http.SdServerGpu
import com.ronjunevaldoz.graphyn.plugins.stablesd.http.SdServerJob
import com.ronjunevaldoz.graphyn.plugins.stablesd.http.SdServerStatus

/**
 * [SdServerControl] backed by the published [ServerSdClient], mapped to `app:shared`'s editor-facing
 * models so the status widget's contract stays stable while the HTTP/readiness logic itself lives
 * in `plugins/stable-diffusion` alongside generation.
 */
class HttpSdServerControl(
    settingsStore: FileSettingsStore = FileSettingsStore(),
) : SdServerControl {
    private val client = ServerSdClient(settingsStore)

    override suspend fun ping(): Boolean = client.ping()
    override suspend fun status(): SdServerStatusModel? = client.status()?.toModel()
    override suspend fun jobs(): List<SdServerJobModel> = client.jobs().map { it.toModel() }
    override suspend fun cancel(jobId: String): Boolean = client.cancel(jobId)
    override suspend fun unload(): SdServerStatusModel? = client.unload()?.toModel()
}

private fun SdServerStatus.toModel() = SdServerStatusModel(
    cpuCores = cpuCores,
    ramTotalMb = ramTotalMb,
    ramAvailableMb = ramAvailableMb,
    gpus = gpus.map { it.toModel() },
    loadedModel = loadedModel,
    busy = busy,
)

private fun SdServerGpu.toModel() = SdGpu(
    name = name,
    vramTotalMb = vramTotalMb,
    vramUsedMb = vramUsedMb,
    vramFreeMb = vramFreeMb,
    utilizationPct = utilizationPct,
)

private fun SdServerJob.toModel() = SdServerJobModel(
    id = id,
    workflowId = workflowId,
    state = state,
    submittedAt = submittedAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    error = error,
)
