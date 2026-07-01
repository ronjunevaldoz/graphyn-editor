package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.ArtifactRecord
import com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
import java.io.File

/** Generation-input flags recorded (without their `--` prefix) for artifact provenance. */
private val INPUT_FLAGS = listOf(
    "--steps", "--seed", "--width", "--height", "--cfg-scale", "--guidance", "--flow-shift",
    "--sampling-method", "--strength", "--negative-prompt", "--video-frames", "--fps", "--max-vram",
)

/**
 * Builds an [ArtifactRecord] for a generated file: the prompt, diffusion model, key generation
 * inputs (steps/seed/size/…), the elapsed time, and the workflow it came from (via
 * [SdArtifactContext]) — so the artifact history is searchable and reproducible.
 */
internal fun buildArtifactRecord(
    file: File,
    kind: ArtifactKind,
    args: List<String>,
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
    prompt = promptOf(args),
    model = File(valueOf(args, "--diffusion-model").orEmpty()).name.ifEmpty { null },
    elapsedMs = elapsedMs,
    inputs = INPUT_FLAGS.mapNotNull { flag -> valueOf(args, flag)?.let { flag.removePrefix("--") to it } }.toMap()
        + lorasInput(args),
)

/** LoRA file names (comma-joined) from inline <lora:…> prompt tags, when present. */
private fun lorasInput(args: List<String>): Map<String, String> {
    val loras = args.flatMap { LORA_TAG.findAll(it).toList() }
        .map { it.value.removePrefix("<lora:").substringBefore(':').substringAfterLast('/').substringAfterLast('\\') }
    return if (loras.isEmpty()) emptyMap() else mapOf("loras" to loras.joinToString(","))
}

private val LORA_TAG = Regex("<lora:[^>]*>")

/** The positive prompt with any inline `<lora:…>` tags stripped, or null when absent. */
private fun promptOf(args: List<String>): String? {
    val prompts = args.withIndex().filter { it.value == "--prompt" }
        .mapNotNull { args.getOrNull(it.index + 1) }
    return prompts.lastOrNull { !it.startsWith("<lora:") }
        ?.replace(LORA_TAG, "")?.trim()?.ifEmpty { null }
}

private fun valueOf(args: List<String>, flag: String): String? {
    val i = args.indexOf(flag)
    return if (i >= 0) args.getOrNull(i + 1) else null
}
