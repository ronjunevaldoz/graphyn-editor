package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.ListValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.NullValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.asRecord
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.orEmpty

internal fun txt2imgExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val args = buildImageArgs(inputs, initImagePath = null)
    val result = backend.generateImage(args)
    val paths = result.imagePaths.map { StringValue(it) }
    mapOf(
        "images" to ListValue(paths),
        "image"  to (paths.firstOrNull() ?: WorkflowValue.NullValue),
    )
}

internal fun img2imgExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val initImage = (inputs["init_image"] as? StringValue)?.value
        ?: error("sd.img2img: init_image is required")
    val strength = when (val v = inputs["strength"]) {
        is WorkflowValue.DoubleValue -> v.value
        is WorkflowValue.IntValue    -> v.value.toDouble()
        else -> 0.75
    }
    val args = buildImageArgs(inputs, initImagePath = initImage) +
        listOf("--strength", strength.toString())
    val result = backend.generateImage(args)
    val paths = result.imagePaths.map { StringValue(it) }
    mapOf(
        "images" to ListValue(paths),
        "image"  to (paths.firstOrNull() ?: WorkflowValue.NullValue),
    )
}

private fun buildImageArgs(
    inputs: Map<String, WorkflowValue>,
    initImagePath: String?,
): List<String> = buildList {
    val ctxFields    = inputs["context"]?.asRecord("sd.context") ?: error("sd context required")
    val samplerFields = inputs["sampler"]?.asRecord("sd.sampler") ?: error("sd sampler required")

    addAll(buildContextArgs(ctxFields))
    addAll(buildSamplerArgs(samplerFields))

    inputs["hires"]?.let { if (it !is NullValue) addAll(buildHiresArgs(it.orEmpty())) }
    inputs["cache"]?.let { if (it !is NullValue) addAll(buildCacheArgs(it.orEmpty())) }
    inputs["vae_tiling"]?.let { if (it !is NullValue) addAll(buildTilingArgs(it.orEmpty())) }

    (inputs["loras"] as? ListValue)?.items?.forEach { loraToken ->
        val lora = loraToken.orEmpty()
        val path = (lora["path"] as? StringValue)?.value ?: return@forEach
        val mult = when (val m = lora["multiplier"]) {
            is WorkflowValue.DoubleValue -> m.value
            is WorkflowValue.IntValue    -> m.value.toDouble()
            else -> 1.0
        }
        add("--prompt"); add("<lora:$path:$mult>")
    }

    (inputs["prompt"] as? StringValue)?.value?.let { add("--prompt"); add(it) }
    (inputs["negative_prompt"] as? StringValue)?.value?.let { add("--negative-prompt"); add(it) }
    (inputs["width"] as? WorkflowValue.IntValue)?.value?.takeIf { it > 0 }
        ?.let { add("--width"); add(it.toString()) }
    (inputs["height"] as? WorkflowValue.IntValue)?.value?.takeIf { it > 0 }
        ?.let { add("--height"); add(it.toString()) }
    (inputs["seed"] as? WorkflowValue.IntValue)?.value?.let { add("--seed"); add(it.toString()) }
    (inputs["batch_count"] as? WorkflowValue.IntValue)?.value?.let { add("--batch-count"); add(it.toString()) }
    (inputs["clip_skip"] as? WorkflowValue.IntValue)?.value?.takeIf { it > 0 }
        ?.let { add("--clip-skip"); add(it.toString()) }

    initImagePath?.let { add("--init-img"); add(it) }

    if ((inputs["embed_image_metadata"] as? BooleanValue)?.value == false) add("--disable-image-metadata")

    inputs["controlnet"]?.let { if (it !is NullValue) addAll(buildControlNetArgs(it.orEmpty())) }
    inputs["id_cond"]?.let { if (it !is NullValue) addAll(buildIdCondArgs(it.orEmpty())) }
}
