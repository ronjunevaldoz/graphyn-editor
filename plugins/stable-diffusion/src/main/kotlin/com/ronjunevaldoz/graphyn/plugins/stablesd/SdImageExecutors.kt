package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.ListValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.asRecord

internal fun txt2imgExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val request = buildImageRequest(inputs, initImagePath = null, strength = null)
    val result = backend.generateImage(request)
    val paths = result.imagePaths.map { StringValue(it) }
    mapOf(
        "images" to ListValue(paths),
        "image" to (paths.firstOrNull() ?: WorkflowValue.NullValue),
    )
}

internal fun img2imgExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val initImage = (inputs["init_image"] as? StringValue)?.value
        ?: error("sd.img2img: init_image is required")
    val strength = when (val v = inputs["strength"]) {
        is WorkflowValue.DoubleValue -> v.value
        is WorkflowValue.IntValue -> v.value.toDouble()
        else -> 0.75
    }
    val request = buildImageRequest(inputs, initImagePath = initImage, strength = strength)
    val result = backend.generateImage(request)
    val paths = result.imagePaths.map { StringValue(it) }
    mapOf(
        "images" to ListValue(paths),
        "image" to (paths.firstOrNull() ?: WorkflowValue.NullValue),
    )
}

private fun buildImageRequest(
    inputs: Map<String, WorkflowValue>,
    initImagePath: String?,
    strength: Double?,
): SdGenerateImageRequest {
    val ctxFields = inputs["context"]?.asRecord("sd.context") ?: error("sd context required")
    val samplerFields = inputs["sampler"]?.asRecord("sd.sampler") ?: error("sd sampler required")

    return SdGenerateImageRequest(
        prompt = (inputs["prompt"] as? StringValue)?.value ?: "",
        negativePrompt = (inputs["negative_prompt"] as? StringValue)?.value ?: "",
        width = (inputs["width"] as? WorkflowValue.IntValue)?.value?.takeIf { it > 0 },
        height = (inputs["height"] as? WorkflowValue.IntValue)?.value?.takeIf { it > 0 },
        seed = (inputs["seed"] as? WorkflowValue.IntValue)?.value?.toLong() ?: -1L,
        batchCount = (inputs["batch_count"] as? WorkflowValue.IntValue)?.value ?: 1,
        clipSkip = (inputs["clip_skip"] as? WorkflowValue.IntValue)?.value?.takeIf { it > 0 },
        initImagePath = initImagePath,
        strength = strength,
        embedImageMetadata = (inputs["embed_image_metadata"] as? BooleanValue)?.value ?: true,
        context = extractContextConfig(ctxFields),
        sampler = extractSamplerConfig(samplerFields),
        hires = extractHiresConfig(inputs["hires"]),
        cache = extractCacheConfig(inputs["cache"]),
        vaeTiling = extractTilingConfig(inputs["vae_tiling"]),
        controlNet = extractControlNetConfig(inputs["controlnet"]),
        idCond = extractIdCondConfig(inputs["id_cond"]),
        loras = extractLoras(inputs),
    )
}
