package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.ListValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.NullValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.asRecord

internal fun txt2vidExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val request = buildVideoRequest(inputs, initImagePath = null, endImagePath = null, strength = null)
    val result = backend.generateVideo(request)
    mapOf(
        "frames" to ListValue(result.framePaths.map { StringValue(it) }),
        "audio_path" to (result.audioPath?.let { StringValue(it) } ?: NullValue),
    )
}

internal fun img2vidExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val initImage = (inputs["init_image"] as? StringValue)?.value
        ?: error("sd.img2vid: init_image is required")
    val endImage = (inputs["end_image"] as? StringValue)?.value
    val strength = when (val v = inputs["strength"]) {
        is DoubleValue -> v.value
        is IntValue -> v.value.toDouble()
        else -> 0.75
    }
    val request = buildVideoRequest(inputs, initImage, endImage, strength)
    val result = backend.generateVideo(request)
    mapOf(
        "frames" to ListValue(result.framePaths.map { StringValue(it) }),
        "audio_path" to (result.audioPath?.let { StringValue(it) } ?: NullValue),
    )
}

private fun buildVideoRequest(
    inputs: Map<String, WorkflowValue>,
    initImagePath: String?,
    endImagePath: String?,
    strength: Double?,
): SdGenerateVideoRequest {
    val ctxFields = inputs["context"]?.asRecord("sd.context") ?: error("sd context required")
    val samplerFields = inputs["sampler"]?.asRecord("sd.sampler") ?: error("sd sampler required")
    val highNoiseToken = inputs["high_noise_sampler"]
    val highNoiseSampler = if (highNoiseToken != null && highNoiseToken !is NullValue) {
        extractSamplerConfig(highNoiseToken.asRecord("sd.sampler"))
    } else null

    return SdGenerateVideoRequest(
        prompt = (inputs["prompt"] as? StringValue)?.value ?: "",
        negativePrompt = (inputs["negative_prompt"] as? StringValue)?.value ?: "",
        width = (inputs["width"] as? IntValue)?.value?.takeIf { it > 0 },
        height = (inputs["height"] as? IntValue)?.value?.takeIf { it > 0 },
        seed = (inputs["seed"] as? IntValue)?.value?.toLong() ?: -1L,
        clipSkip = (inputs["clip_skip"] as? IntValue)?.value?.takeIf { it > 0 },
        videoFrames = (inputs["video_frames"] as? IntValue)?.value,
        fps = (inputs["fps"] as? IntValue)?.value,
        moeBoundary = (inputs["moe_boundary"] as? DoubleValue)?.value,
        vaceStrength = (inputs["vace_strength"] as? DoubleValue)?.value,
        embedImageMetadata = (inputs["embed_image_metadata"] as? BooleanValue)?.value ?: true,
        initImagePath = initImagePath,
        endImagePath = endImagePath,
        strength = strength,
        controlFrames = (inputs["control_frames"] as? ListValue)?.items
            ?.filterIsInstance<StringValue>()?.map { it.value }.orEmpty(),
        context = extractContextConfig(ctxFields),
        sampler = extractSamplerConfig(samplerFields),
        highNoiseSampler = highNoiseSampler,
        hires = extractHiresConfig(inputs["hires"]),
        cache = extractCacheConfig(inputs["cache"]),
        vaeTiling = extractTilingConfig(inputs["vae_tiling"]),
        loras = extractLoras(inputs),
    )
}
