package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import kotlinx.serialization.Serializable

/** Wire shape for `POST /api/sd/load` — mirrors server-sd's own `LoadModelRequest`. */
@Serializable
data class LoadModelRequest(
    val diffusionModelPath: String,
    val clipLPath: String = "",
    val clipGPath: String = "",
    val t5xxlPath: String = "",
    val vaePath: String = "",
    val llmPath: String = "",
    val llmVisionPath: String = "",
    val qwenImageZeroCondT: Boolean = false,
    val backend: String = "",
    val nThreads: Int = -1,
    val diffusionFa: Boolean = true,
    val maxVram: String = "",
)
