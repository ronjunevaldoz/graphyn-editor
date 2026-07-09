package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val SCHEDULERS = mapOf(
    "discrete" to 0, "karras" to 1, "exponential" to 2, "ays" to 3,
    "gits" to 4, "sgm_uniform" to 5, "simple" to 6, "smoothstep" to 7,
    "kl_optimal" to 8, "lcm" to 9,
)

private val CACHE_MODES = mapOf("disabled" to 0, "delta_cache" to 1, "spectrum_cache" to 2)

private val requestJson = Json { encodeDefaults = true }

/**
 * Mirrors server-sd's `GenerateExRequest` (`SdGenerateRoutes.kt`) field-for-field. Only carries the
 * subset of [SdGenerateImageRequest]'s fields the server's `/api/sd/generate-ex` contract actually
 * accepts — many `sd.context`/`sd.sampler`/etc ports (rng_type, prediction, custom_sigmas, slg
 * details, chroma_*, circular*, ...) have no server-side counterpart yet and are intentionally left
 * unmapped, an explicit, visible gap rather than a silently dropped CLI flag.
 */
@Serializable
private data class GenerateExRequestBody(
    val prompt: String,
    val negativePrompt: String = "",
    val diffusionModelPath: String = "",
    // All-in-one checkpoint path (SDXL etc.) — loaded with no tensor-name prefix. Set this OR
    // diffusionModelPath, not both. See StableDiffusionConfig.modelPath's doc (sibling repo).
    val modelPath: String = "",
    val clipLPath: String = "",
    val clipGPath: String = "",
    val t5xxlPath: String = "",
    val vaePath: String = "",
    val llmPath: String = "",
    val llmVisionPath: String = "",
    val photoMakerPath: String = "",
    val pulidWeightsPath: String = "",
    val qwenImageZeroCondT: Boolean = false,
    val backend: String = "",
    val nThreads: Int = -1,
    val diffusionFa: Boolean = true,
    val maxVram: String = "",
    val streamLayers: Boolean = false,
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 20,
    val seed: Long = -1L,
    val cfgScale: Float = 7.5f,
    val samplingMethod: String = "euler",
    val scheduler: Int = 0,
    val flowShift: Float = 3.0f,
    val strength: Float = 0.6f,
    val initImagePath: String = "",
    val batchCount: Int = 1,
    val clipSkip: Int = 0,
    val imgCfg: Float = 0f,
    val distilledGuidance: Float = 0f,
    val eta: Float = 0f,
    val shiftedTimestep: Int = 0,
    val loraPaths: List<String> = emptyList(),
    val loraMultipliers: List<Float> = emptyList(),
    val refImagePaths: List<String> = emptyList(),
    val pmIdEmbedPath: String = "",
    val pmStyleStrength: Float = 20f,
    val pulidIdEmbeddingPath: String = "",
    val pulidIdWeight: Float = 1f,
    val maskImagePath: String = "",
    val controlImagePath: String = "",
    val controlStrength: Float = 0.9f,
    val vaeTilingEnabled: Boolean = false,
    val cacheMode: Int = 0,
    val hiresEnabled: Boolean = false,
    val hiresScale: Float = 2f,
    val hiresSteps: Int = 0,
    val hiresDenoisingStrength: Float = 0.7f,
)

/** Mirrors server-sd's `GenerateVideoRequest` (`SdVideoRoutes.kt`) field-for-field. */
@Serializable
private data class GenerateVideoRequestBody(
    val prompt: String,
    val negativePrompt: String = "",
    val initImagePath: String,
    val lowNoiseModelPath: String = "",
    val highNoiseModelPath: String = "",
    val clipVisionPath: String = "",
    val textEncoderPath: String = "",
    val vaePath: String = "",
    // LTX model paths (server-side). No env-var fallback — LTX requires an explicit modelProfile.
    val llmPath: String = "",
    val embeddingsConnectorsPath: String = "",
    val audioVaePath: String = "",
    val width: Int = 832,
    val height: Int = 480,
    val videoFrames: Int = 81,
    val fps: Int = 16,
    val steps: Int = 4,
    val highNoiseSteps: Int = 0,
    val moeBoundary: Float = 0.5f,
    val seed: Long = -1L,
    val cfgScale: Float = 1.0f,
    val flowShift: Float = 5.0f,
    val samplingMethod: String = "euler",
    val loraPaths: List<String> = emptyList(),
    val loraMultipliers: List<Float> = emptyList(),
    val loraHighNoise: List<Boolean> = emptyList(),
)

internal fun imageRequestToJson(request: SdGenerateImageRequest): String {
    val ctx = request.context
    val body = GenerateExRequestBody(
        prompt = request.prompt,
        negativePrompt = request.negativePrompt,
        diffusionModelPath = ctx.diffusionModelPath.orEmpty(),
        modelPath = ctx.modelPath.orEmpty(),
        clipLPath = ctx.clipLPath.orEmpty(),
        clipGPath = ctx.clipGPath.orEmpty(),
        t5xxlPath = ctx.t5xxlPath.orEmpty(),
        vaePath = ctx.vaePath.orEmpty(),
        llmPath = ctx.llmPath.orEmpty(),
        llmVisionPath = ctx.llmVisionPath.orEmpty(),
        photoMakerPath = ctx.photoMakerPath.orEmpty(),
        pulidWeightsPath = ctx.pulidWeightsPath.orEmpty(),
        qwenImageZeroCondT = ctx.qwenImageZeroCondT,
        backend = ctx.backend.orEmpty(),
        nThreads = ctx.nThreads ?: -1,
        diffusionFa = ctx.diffusionFlashAttn || ctx.flashAttn,
        maxVram = ctx.maxVram.orEmpty(),
        streamLayers = ctx.streamLayers,
        width = request.width ?: 512,
        height = request.height ?: 512,
        steps = request.sampler.sampleSteps ?: 20,
        seed = request.seed,
        cfgScale = (request.sampler.txtCfg ?: 7.5).toFloat(),
        samplingMethod = request.sampler.sampleMethod ?: "euler",
        scheduler = SCHEDULERS[request.sampler.scheduler] ?: 0,
        flowShift = (request.sampler.flowShift ?: 3.0).toFloat(),
        strength = (request.strength ?: 0.6).toFloat(),
        initImagePath = request.initImagePath.orEmpty(),
        batchCount = request.batchCount,
        clipSkip = request.clipSkip ?: 0,
        imgCfg = (request.sampler.imgCfg ?: 0.0).toFloat(),
        distilledGuidance = (request.sampler.distilledGuidance ?: 0.0).toFloat(),
        eta = (request.sampler.eta ?: 0.0).toFloat(),
        shiftedTimestep = request.sampler.shiftedTimestep ?: 0,
        loraPaths = request.loras.map { it.path },
        loraMultipliers = request.loras.map { it.multiplier.toFloat() },
        refImagePaths = request.idCond?.refImages.orEmpty(),
        pmIdEmbedPath = request.idCond?.pmIdEmbedPath.orEmpty(),
        pmStyleStrength = (request.idCond?.pmStyleStrength ?: 20.0).toFloat(),
        pulidIdEmbeddingPath = request.idCond?.pulidIdEmbeddingPath.orEmpty(),
        pulidIdWeight = (request.idCond?.pulidIdWeight ?: 1.0).toFloat(),
        maskImagePath = request.controlNet?.maskImage.orEmpty(),
        controlImagePath = request.controlNet?.controlImage.orEmpty(),
        controlStrength = (request.controlNet?.controlStrength ?: 0.9).toFloat(),
        vaeTilingEnabled = request.vaeTiling?.enabled == true,
        cacheMode = CACHE_MODES[request.cache?.mode] ?: 0,
        hiresEnabled = request.hires?.enabled == true,
        hiresScale = (request.hires?.scale ?: 2.0).toFloat(),
        hiresSteps = request.hires?.steps ?: 0,
        hiresDenoisingStrength = (request.hires?.denoisingStrength ?: 0.7).toFloat(),
    )
    return requestJson.encodeToString(GenerateExRequestBody.serializer(), body)
}

internal fun videoRequestToJson(request: SdGenerateVideoRequest): String {
    val ctx = request.context
    val body = GenerateVideoRequestBody(
        prompt = request.prompt,
        negativePrompt = request.negativePrompt,
        initImagePath = request.initImagePath.orEmpty(),
        lowNoiseModelPath = ctx.diffusionModelPath.orEmpty(),
        highNoiseModelPath = ctx.highNoiseDiffusionModelPath.orEmpty(),
        clipVisionPath = ctx.clipVisionPath.orEmpty(),
        textEncoderPath = ctx.t5xxlPath.orEmpty(),
        vaePath = ctx.vaePath.orEmpty(),
        llmPath = ctx.llmPath.orEmpty(),
        embeddingsConnectorsPath = ctx.embeddingsConnectorsPath.orEmpty(),
        audioVaePath = ctx.audioVaePath.orEmpty(),
        width = request.width ?: 832,
        height = request.height ?: 480,
        videoFrames = request.videoFrames ?: 81,
        fps = request.fps ?: 16,
        steps = request.sampler.sampleSteps ?: 4,
        highNoiseSteps = request.highNoiseSampler?.sampleSteps ?: 0,
        moeBoundary = (request.moeBoundary ?: 0.5).toFloat(),
        seed = request.seed,
        cfgScale = (request.sampler.txtCfg ?: 1.0).toFloat(),
        flowShift = (request.sampler.flowShift ?: 5.0).toFloat(),
        samplingMethod = request.sampler.sampleMethod ?: "euler",
        loraPaths = request.loras.map { it.path },
        loraMultipliers = request.loras.map { it.multiplier.toFloat() },
        loraHighNoise = request.loras.map { it.path.contains("high_noise") },
    )
    return requestJson.encodeToString(GenerateVideoRequestBody.serializer(), body)
}
