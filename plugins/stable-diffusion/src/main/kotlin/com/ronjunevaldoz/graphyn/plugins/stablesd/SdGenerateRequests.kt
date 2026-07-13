package com.ronjunevaldoz.graphyn.plugins.stablesd

import kotlinx.serialization.Serializable

/**
 * Typed replacement for the old `List<String>` of `--flag value` CLI tokens. Node executors
 * build these directly from typed [com.ronjunevaldoz.graphyn.core.model.WorkflowValue] inputs —
 * no intermediate string representation, no flag-name matching, no silent-drop parsing bugs.
 * Comprehensive (mirrors the full sd-cli flag surface, matching every `sd.*` NodeSpec field) so
 * [SdCliBackend] — a real CLI-process backend used by other consumers of this published plugin —
 * keeps full fidelity. [com.ronjunevaldoz.graphyn.bootstrap.HttpStableDiffusionBackend] (app/demo)
 * only extracts the narrower subset server-sd's `/api/sd/generate-ex` JSON contract accepts.
 */

@Serializable
data class SdContextConfig(
    val modelPath: String? = null,
    val clipLPath: String? = null,
    val clipGPath: String? = null,
    val clipVisionPath: String? = null,
    val t5xxlPath: String? = null,
    val llmPath: String? = null,
    val llmVisionPath: String? = null,
    val diffusionModelPath: String? = null,
    val highNoiseDiffusionModelPath: String? = null,
    val uncondDiffusionModelPath: String? = null,
    val embeddingsConnectorsPath: String? = null,
    val vaePath: String? = null,
    val vaeFormat: String? = null,
    val audioVaePath: String? = null,
    val taesdPath: String? = null,
    val esrganPath: String? = null,
    val controlNetPath: String? = null,
    val embeddingDir: String? = null,
    val photoMakerPath: String? = null,
    val pulidWeightsPath: String? = null,
    val loraModelDir: String? = null,
    val hiresUpscalersDir: String? = null,
    val tensorTypeRules: String? = null,
    val wtype: String? = null,
    val nThreads: Int? = null,
    val rngType: String? = null,
    val samplerRngType: String? = null,
    val prediction: String? = null,
    val loraApplyMode: String? = null,
    val offloadParamsToCpu: Boolean = false,
    val maxVram: String? = null,
    val backend: String? = null,
    val paramsBackend: String? = null,
    val rpcServers: String? = null,
    val enableMmap: Boolean = false,
    val flashAttn: Boolean = false,
    val diffusionFlashAttn: Boolean = false,
    val diffusionConvDirect: Boolean = false,
    val vaeConvDirect: Boolean = false,
    val clipOnCpu: Boolean = false,
    val vaeOnCpu: Boolean = false,
    val controlNetCpu: Boolean = false,
    val streamLayers: Boolean = false,
    val eagerLoad: Boolean = false,
    val circular: Boolean = false,
    val circularX: Boolean = false,
    val circularY: Boolean = false,
    val forceSdxlVaeConvScale: Boolean = false,
    val chromaUseDitMask: Boolean? = null,
    val chromaUseT5Mask: Boolean = false,
    val chromaT5MaskPad: Int? = null,
    val qwenImageZeroCondT: Boolean = false,
)

@Serializable
data class SdSamplerConfig(
    val sampleMethod: String? = null,
    val scheduler: String? = null,
    val sampleSteps: Int? = null,
    val txtCfg: Double? = null,
    val imgCfg: Double? = null,
    val distilledGuidance: Double? = null,
    val eta: Double? = null,
    val flowShift: Double? = null,
    val shiftedTimestep: Int? = null,
    val customSigmas: List<Double>? = null,
    val extraSampleArgs: String? = null,
    val slgLayers: List<Int>? = null,
    val slgLayerStart: Double? = null,
    val slgLayerEnd: Double? = null,
    val slgScale: Double? = null,
)

@Serializable
data class SdHiresConfig(
    val enabled: Boolean = false,
    val upscaler: String? = null,
    val modelPath: String? = null,
    val scale: Double? = null,
    val targetWidth: Int? = null,
    val targetHeight: Int? = null,
    val steps: Int? = null,
    val denoisingStrength: Double? = null,
    val upscaleTileSize: Int? = null,
    val customSigmas: List<Double>? = null,
)

@Serializable
data class SdCacheConfig(
    val mode: String? = null,
    val reuseThreshold: Double? = null,
    val startPercent: Double? = null,
    val endPercent: Double? = null,
    val errorDecayRate: Double? = null,
    val useRelativeThreshold: Boolean? = null,
    val resetErrorOnCompute: Boolean? = null,
    val fnComputeBlocks: Int? = null,
    val bnComputeBlocks: Int? = null,
    val maxWarmupSteps: Int? = null,
    val spectrumW: Double? = null,
    val spectrumM: Int? = null,
    val spectrumLam: Double? = null,
    val spectrumWindowSize: Int? = null,
    val spectrumFlexWindow: Double? = null,
    val spectrumWarmupSteps: Int? = null,
    val spectrumStopPercent: Double? = null,
    val scmMask: String? = null,
    val scmPolicyDynamic: Boolean? = null,
)

@Serializable
data class SdTilingConfig(
    val enabled: Boolean = false,
    val temporalTiling: Boolean = false,
    val tileSizeX: Int? = null,
    val targetOverlap: Double? = null,
    val relSizeX: Double? = null,
    val extraTilingArgs: String? = null,
)

@Serializable
data class SdControlNetConfig(
    val controlImage: String? = null,
    val controlStrength: Double? = null,
    val maskImage: String? = null,
)

@Serializable
data class SdIdCondConfig(
    val refImages: List<String> = emptyList(),
    val autoResizeRefImage: Boolean? = null,
    val increaseRefIndex: Boolean = false,
    val pmIdEmbedPath: String? = null,
    val pmIdImagesDir: String? = null,
    val pmStyleStrength: Double? = null,
    val pulidIdEmbeddingPath: String? = null,
    val pulidIdWeight: Double? = null,
)

@Serializable
data class SdLoraConfig(val path: String, val multiplier: Double = 1.0)

/**
 * Optional per-generation override for which `server-sd` instance to talk to — lets one workflow
 * mix multiple deployments (e.g. a local box and a Modal-hosted `server-sd`) by wiring an
 * `sd.server` node into a generation node's `server` port. Only the HTTP backend honors this;
 * a CLI-based backend shells out locally and ignores it. Null/blank fields fall back to the
 * app-wide setting → env var → `127.0.0.1:5000` default.
 */
@Serializable
data class SdServerConfig(val baseUrl: String? = null, val apiKey: String? = null)

@Serializable
data class SdGenerateImageRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val seed: Long = -1L,
    val batchCount: Int = 1,
    val clipSkip: Int? = null,
    val initImagePath: String? = null,
    val strength: Double? = null,
    val embedImageMetadata: Boolean = true,
    val context: SdContextConfig = SdContextConfig(),
    val sampler: SdSamplerConfig = SdSamplerConfig(),
    val hires: SdHiresConfig? = null,
    val cache: SdCacheConfig? = null,
    val vaeTiling: SdTilingConfig? = null,
    val controlNet: SdControlNetConfig? = null,
    val idCond: SdIdCondConfig? = null,
    val loras: List<SdLoraConfig> = emptyList(),
    val server: SdServerConfig? = null,
)

@Serializable
data class SdGenerateVideoRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val seed: Long = -1L,
    val clipSkip: Int? = null,
    val videoFrames: Int? = null,
    val fps: Int? = null,
    val moeBoundary: Double? = null,
    val vaceStrength: Double? = null,
    val embedImageMetadata: Boolean = true,
    val initImagePath: String? = null,
    val endImagePath: String? = null,
    val strength: Double? = null,
    val controlFrames: List<String> = emptyList(),
    val context: SdContextConfig = SdContextConfig(),
    val sampler: SdSamplerConfig = SdSamplerConfig(),
    val highNoiseSampler: SdSamplerConfig? = null,
    val hires: SdHiresConfig? = null,
    val cache: SdCacheConfig? = null,
    val vaeTiling: SdTilingConfig? = null,
    val loras: List<SdLoraConfig> = emptyList(),
    val server: SdServerConfig? = null,
)
