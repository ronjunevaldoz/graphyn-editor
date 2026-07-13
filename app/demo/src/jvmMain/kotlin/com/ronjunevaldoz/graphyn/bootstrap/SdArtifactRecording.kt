package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.ArtifactRecord
import com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest
import java.io.File

/**
 * Builds an [ArtifactRecord] for a generated image: the prompt, diffusion model, key generation
 * inputs (steps/seed/size/…), the elapsed time, and the workflow it came from (via
 * [SdArtifactContext]) — so the artifact history is searchable and reproducible.
 */
internal fun buildArtifactRecord(
    file: File,
    kind: ArtifactKind,
    request: SdGenerateImageRequest,
    elapsedMs: Long,
    nodeType: String,
): ArtifactRecord = ArtifactRecord(
    id = file.name,
    kind = kind,
    path = file.absolutePath,
    createdAt = System.currentTimeMillis(),
    workflowId = SdArtifactContext.workflowId,
    workflowName = SdArtifactContext.workflowName,
    nodeType = nodeType,
    prompt = request.prompt.ifBlank { null },
    model = request.context.diffusionModelPath?.let { File(it).name }?.ifEmpty { null },
    elapsedMs = elapsedMs,
    inputs = buildMap {
        request.sampler.sampleSteps?.let { put("steps", it.toString()) }
        put("seed", request.seed.toString())
        request.width?.let { put("width", it.toString()) }
        request.height?.let { put("height", it.toString()) }
        request.sampler.txtCfg?.let { put("cfg-scale", it.toString()) }
        request.sampler.distilledGuidance?.let { put("guidance", it.toString()) }
        request.sampler.flowShift?.let { put("flow-shift", it.toString()) }
        request.sampler.sampleMethod?.let { put("sampling-method", it) }
        request.strength?.let { put("strength", it.toString()) }
        request.negativePrompt.takeIf { it.isNotBlank() }?.let { put("negative-prompt", it) }
        if (request.loras.isNotEmpty()) put("loras", request.loras.joinToString(",") { File(it.path).name })
    },
)

/** Builds an [ArtifactRecord] for a generated video — same shape as the image overload, video-specific fields. */
internal fun buildArtifactRecord(
    file: File,
    kind: ArtifactKind,
    request: SdGenerateVideoRequest,
    elapsedMs: Long,
    nodeType: String,
): ArtifactRecord = ArtifactRecord(
    id = file.name,
    kind = kind,
    path = file.absolutePath,
    createdAt = System.currentTimeMillis(),
    workflowId = SdArtifactContext.workflowId,
    workflowName = SdArtifactContext.workflowName,
    nodeType = nodeType,
    prompt = request.prompt.ifBlank { null },
    model = request.context.diffusionModelPath?.let { File(it).name }?.ifEmpty { null },
    elapsedMs = elapsedMs,
    inputs = buildMap {
        request.sampler.sampleSteps?.let { put("steps", it.toString()) }
        put("seed", request.seed.toString())
        request.width?.let { put("width", it.toString()) }
        request.height?.let { put("height", it.toString()) }
        request.sampler.txtCfg?.let { put("cfg-scale", it.toString()) }
        request.sampler.flowShift?.let { put("flow-shift", it.toString()) }
        request.sampler.sampleMethod?.let { put("sampling-method", it) }
        request.videoFrames?.let { put("video-frames", it.toString()) }
        request.fps?.let { put("fps", it.toString()) }
        request.context.maxVram?.let { put("max-vram", it) }
        request.negativePrompt.takeIf { it.isNotBlank() }?.let { put("negative-prompt", it) }
        if (request.loras.isNotEmpty()) put("loras", request.loras.joinToString(",") { File(it.path).name })
    },
)
