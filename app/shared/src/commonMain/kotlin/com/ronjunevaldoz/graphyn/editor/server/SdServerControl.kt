package com.ronjunevaldoz.graphyn.editor.server

import kotlinx.serialization.Serializable

/** One GPU's live state, as reported by the SD server. */
@Serializable
data class SdGpu(
    val name: String,
    val vramTotalMb: Int,
    val vramUsedMb: Int,
    val vramFreeMb: Int,
    val utilizationPct: Int,
)

/** Snapshot of the SD server: hardware, the currently-loaded model, and whether it's generating. */
@Serializable
data class SdServerStatusModel(
    val cpuCores: Int = 0,
    val ramTotalMb: Long = 0,
    val ramAvailableMb: Long = 0,
    val gpus: List<SdGpu> = emptyList(),
    val loadedModel: String? = null,
    val busy: Boolean = false,
)

/** One async SD job exposed by the worker for lifecycle and cancelation checks. */
@Serializable
data class SdServerJobModel(
    val id: String,
    val workflowId: String? = null,
    val state: String,
    val submittedAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val error: String? = null,
)

/**
 * Editor-facing control over the SD inference server: read live status and free the loaded model.
 * Implemented on the JVM host (HTTP to server-sd); passed into the shell so a status widget can
 * poll it. Null when the app has no SD server configured.
 */
interface SdServerControl {
    /** Fast health probe for worker reachability. */
    suspend fun ping(): Boolean

    /** Current server status, or null when unreachable. */
    suspend fun status(): SdServerStatusModel?

    /** Current worker job list, or empty when unreachable. */
    suspend fun jobs(): List<SdServerJobModel>

    /** Requests cancelation of a worker job; returns true when the worker accepts it. */
    suspend fun cancel(jobId: String): Boolean

    /** Frees the loaded model's VRAM; returns the post-unload status (or null on failure). */
    suspend fun unload(): SdServerStatusModel?
}
