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
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.orEmpty

internal fun txt2vidExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val args = buildVideoArgs(inputs, initImagePath = null, endImagePath = null)
    val result = backend.generateVideo(args)
    mapOf(
        "frames"     to ListValue(result.framePaths.map { StringValue(it) }),
        "audio_path" to (result.audioPath?.let { StringValue(it) } ?: NullValue),
    )
}

internal fun img2vidExecutor(backend: StableDiffusionBackend) = NodeExecutor { inputs ->
    val initImage = (inputs["init_image"] as? StringValue)?.value
        ?: error("sd.img2vid: init_image is required")
    val endImage = (inputs["end_image"] as? StringValue)?.value
    val strength = when (val v = inputs["strength"]) {
        is DoubleValue -> v.value
        is IntValue    -> v.value.toDouble()
        else -> 0.75
    }
    val args = buildVideoArgs(inputs, initImage, endImage) +
        listOf("--strength", strength.toString())
    val result = backend.generateVideo(args)
    mapOf(
        "frames"     to ListValue(result.framePaths.map { StringValue(it) }),
        "audio_path" to (result.audioPath?.let { StringValue(it) } ?: NullValue),
    )
}

private fun buildVideoArgs(
    inputs: Map<String, WorkflowValue>,
    initImagePath: String?,
    endImagePath: String?,
): List<String> = buildList {
    val ctxFields     = inputs["context"]?.asRecord("sd.context") ?: error("sd context required")
    val samplerFields = inputs["sampler"]?.asRecord("sd.sampler") ?: error("sd sampler required")

    addAll(buildContextArgs(ctxFields))
    addAll(buildSamplerArgs(samplerFields))

    inputs["high_noise_sampler"]?.let { token ->
        if (token !is NullValue) addAll(buildHighNoiseSamplerArgs(token.asRecord("sd.sampler")))
    }
    inputs["hires"]?.let      { if (it !is NullValue) addAll(buildHiresArgs(it.orEmpty())) }
    inputs["cache"]?.let      { if (it !is NullValue) addAll(buildCacheArgs(it.orEmpty())) }
    inputs["vae_tiling"]?.let { if (it !is NullValue) addAll(buildTilingArgs(it.orEmpty())) }

    (inputs["loras"] as? ListValue)?.items?.forEach { loraToken ->
        val lora = loraToken.orEmpty()
        val path = (lora["path"] as? StringValue)?.value ?: return@forEach
        val mult = when (val m = lora["multiplier"]) {
            is DoubleValue -> m.value
            is IntValue    -> m.value.toDouble()
            else -> 1.0
        }
        add("--prompt"); add("<lora:$path:$mult>")
    }

    (inputs["prompt"] as? StringValue)?.value?.let { add("--prompt"); add(it) }
    (inputs["negative_prompt"] as? StringValue)?.value?.let { add("--negative-prompt"); add(it) }
    (inputs["width"] as? IntValue)?.value?.takeIf { it > 0 }?.let { add("--width"); add(it.toString()) }
    (inputs["height"] as? IntValue)?.value?.takeIf { it > 0 }?.let { add("--height"); add(it.toString()) }
    (inputs["seed"] as? IntValue)?.value?.let { add("--seed"); add(it.toString()) }
    (inputs["clip_skip"] as? IntValue)?.value?.takeIf { it > 0 }
        ?.let { add("--clip-skip"); add(it.toString()) }
    (inputs["video_frames"] as? IntValue)?.value?.let { add("--video-frames"); add(it.toString()) }
    (inputs["fps"] as? IntValue)?.value?.let { add("--fps"); add(it.toString()) }
    (inputs["moe_boundary"] as? DoubleValue)?.value?.let { add("--moe-boundary"); add(it.toString()) }
    (inputs["vace_strength"] as? DoubleValue)?.value?.let { add("--vace-strength"); add(it.toString()) }

    if ((inputs["embed_image_metadata"] as? BooleanValue)?.value == false) add("--disable-image-metadata")

    initImagePath?.let { add("--init-img"); add(it) }
    endImagePath?.let  { add("--end-img"); add(it) }

    (inputs["control_frames"] as? ListValue)?.items?.filterIsInstance<StringValue>()
        ?.forEach { add("--control-video"); add(it.value) }
}
