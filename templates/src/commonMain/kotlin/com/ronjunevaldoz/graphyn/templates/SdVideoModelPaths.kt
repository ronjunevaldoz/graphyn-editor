package com.ronjunevaldoz.graphyn.templates

/**
 * Model file paths for a Wan-style `sd.img2vid`/`sd.txt2vid` graph — mirrors the
 * `sd.diffusion`/`sd.encoders`/`sd.vae`/`sd.offload` node ports for video models.
 */
data class SdVideoModelPaths(
    val lowNoiseModelPath: String,
    val highNoiseModelPath: String? = null,
    val clipVisionPath: String? = null,
    val textEncoderPath: String? = null,
    val vaePath: String? = null,
    val backend: String? = null,
    val nThreads: Int = -1,
    val diffusionFa: Boolean = true,
    /** VRAM budget for graph-cut offload: null = unset (full VRAM), "-1" = auto, "0" = off. */
    val maxVram: String? = null,
    val vaeTiling: Boolean = true,
    val vaeTilingTemporal: Boolean = true,
)

/** Sampler settings for a video generation node — mirrors `sd.sampler`'s ports. */
data class SdVideoSamplingDefaults(
    val steps: Int = 20,
    val cfgScale: Double = 1.0,
    val flowShift: Double? = null,
    /** MoE video models (Wan2.1/2.2) switch to this second sampler past [moeBoundary]. Null = single-pass. */
    val highNoiseSteps: Int? = null,
    val moeBoundary: Double = 0.875,
)
