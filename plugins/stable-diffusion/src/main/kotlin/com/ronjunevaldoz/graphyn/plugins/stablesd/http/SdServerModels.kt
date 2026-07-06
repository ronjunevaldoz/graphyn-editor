package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import kotlinx.serialization.Serializable

/** One GPU's live state, as reported by server-sd's `/api/sd/status`. */
@Serializable
data class SdServerGpu(
    val name: String,
    val vramTotalMb: Int,
    val vramUsedMb: Int,
    val vramFreeMb: Int,
    val utilizationPct: Int,
)

/** Snapshot of server-sd: hardware, the currently-loaded model, and whether it's generating. */
@Serializable
data class SdServerStatus(
    val cpuCores: Int = 0,
    val ramTotalMb: Long = 0,
    val ramAvailableMb: Long = 0,
    val gpus: List<SdServerGpu> = emptyList(),
    val loadedModel: String? = null,
    val busy: Boolean = false,
)

/** One async job exposed by server-sd's `/api/sd/jobs`, for lifecycle and cancelation checks. */
@Serializable
data class SdServerJob(
    val id: String,
    val workflowId: String? = null,
    val state: String,
    val submittedAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val error: String? = null,
)
