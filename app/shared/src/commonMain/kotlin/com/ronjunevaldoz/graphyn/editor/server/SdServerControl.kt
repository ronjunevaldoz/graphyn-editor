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

/**
 * Editor-facing control over the SD inference server: read live status and free the loaded model.
 * Implemented on the JVM host (HTTP to server-sd); passed into the shell so a status widget can
 * poll it. Null when the app has no SD server configured.
 */
interface SdServerControl {
    /** Current server status, or null when unreachable. */
    suspend fun status(): SdServerStatusModel?

    /** Frees the loaded model's VRAM; returns the post-unload status (or null on failure). */
    suspend fun unload(): SdServerStatusModel?
}
