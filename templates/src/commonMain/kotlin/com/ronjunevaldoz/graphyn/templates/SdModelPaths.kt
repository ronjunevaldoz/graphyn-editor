package com.ronjunevaldoz.graphyn.templates

/**
 * Model file paths for one `sd.*` graph — mirrors the `sd.diffusion`/`sd.encoders`/`sd.vae` node
 * ports. Every `sd.*` model family (Flux, Qwen-Image, SD1.5/SDXL, ...) uses the same node wiring;
 * only these paths (and [SdSamplingDefaults]) differ between them.
 */
data class SdModelPaths(
    val diffusionModelPath: String,
    val clipLPath: String? = null,
    val clipGPath: String? = null,
    val t5xxlPath: String? = null,
    val vaePath: String? = null,
    val llmPath: String? = null,
    val llmVisionPath: String? = null,
    val backend: String? = null,
    val nThreads: Int = -1,
    val diffusionFa: Boolean = true,
    val enableMmap: Boolean = true,
    val qwenImageZeroCondT: Boolean = false,
)

/** One `sd.lora` node's config: file path + strength. */
data class SdLoraRef(
    val path: String,
    val multiplier: Double = 1.0,
)

/** Sampler settings shared by every `sd.*` generation node — mirrors `sd.sampler`'s ports. */
data class SdSamplingDefaults(
    val steps: Int = 20,
    val cfgScale: Double = 7.0,
    val distilledGuidance: Double? = null,
    val flowShift: Double? = null,
    val sampleMethod: String? = null,
    val scheduler: String? = null,
    val loras: List<SdLoraRef> = emptyList(),
)
