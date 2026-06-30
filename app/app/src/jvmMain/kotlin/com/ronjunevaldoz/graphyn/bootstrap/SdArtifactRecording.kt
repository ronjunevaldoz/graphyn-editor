package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.ArtifactRecord
import java.io.File

/**
 * Builds an [ArtifactRecord] for a generated file from the executor's CLI-flag args, pulling out
 * the prompt and diffusion-model so the artifact history is searchable. Keeps the HTTP backend thin.
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
    nodeType = nodeType,
    prompt = promptOf(args),
    model = File(valueOf(args, "--diffusion-model").orEmpty()).name.ifEmpty { null },
    elapsedMs = elapsedMs,
)

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
