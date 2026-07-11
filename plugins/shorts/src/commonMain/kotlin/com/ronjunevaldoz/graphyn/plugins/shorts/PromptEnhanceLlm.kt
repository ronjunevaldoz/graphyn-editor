package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr

/**
 * LLM-based alternative to `media.prompt_enhance` (`MediaPromptEnhance.kt`, `plugins/media-ai`) —
 * that node is pure deterministic string-joining (`listOf(prompt, niche, ...).joinToString(", ")`),
 * no actual model call despite the "Enhance" name. This node genuinely expands a rough description
 * via Ollama, reusing [ollamaGenerateUrl]/[ollamaGenerateBody]/[parseOllamaResponse]/[ollamaHttpPost]
 * (the same helpers [ollamaGenerateExecutor] itself is built from) rather than duplicating Ollama
 * HTTP handling.
 *
 * Opt-in per caller (see [mascotSubgraph]'s `useLlmPromptEnhance` param) — not a global default,
 * since it adds a real network call in front of every generation it's wired into, in a pipeline
 * that's already had real infra flakiness this session (Modal 502/303s, local host outages).
 *
 * Falls back to the raw, unmodified [prompt] on ANY failure (Ollama unreachable, malformed
 * response, empty result) — [enhanced] output reports which happened, but generation itself is
 * never blocked by this node's failure the way it would be if enhancement were mandatory.
 */
public val promptEnhanceLlmSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.PROMPT_ENHANCE_LLM,
    label = "Prompt Enhance (LLM)",
    description = "Expands a rough description into a detailed generation prompt via Ollama, with graceful fallback to the raw prompt on failure.",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(
        PortSpec("prompt", WorkflowType.StringType, description = "Rough description to expand"),
        PortSpec("negative_prompt", WorkflowType.StringType, required = false),
        PortSpec("model", WorkflowType.StringType, required = false, description = "Ollama model name (defaults to GRAPHYN_OLLAMA_MODEL or llama3.1)"),
        PortSpec("host", WorkflowType.StringType, required = false, description = "Ollama host base URL (defaults to GRAPHYN_OLLAMA_HOST or http://localhost:11434)"),
    ),
    outputs = listOf(
        PortSpec("prompt", WorkflowType.StringType, description = "Enhanced prompt, or the raw prompt unchanged if enhancement failed"),
        PortSpec("negative_prompt", WorkflowType.StringType),
        PortSpec("enhanced", WorkflowType.BooleanType, description = "True if the LLM call succeeded and prompt is the enhanced version"),
    ),
    defaultValues = mapOf(
        "negative_prompt" to WorkflowValue.StringValue("blurry, low quality, cropped, watermark"),
    ),
)

/** Wraps a rough description in an instruction telling the LLM how to expand it — see this file's
 * top doc comment for what this node does and why. Kept as its own function so tests can assert on
 * the exact instruction text independent of the executor's HTTP/fallback plumbing. */
internal fun promptEnhanceLlmInstruction(roughPrompt: String): String =
    "You are a prompt engineer for Stable Diffusion image generation. Expand the following rough " +
        "description into a detailed, effective generation prompt. Preserve the described subject, " +
        "pose, and composition exactly — do not change what the person asked for. If camera angle " +
        "or viewing direction is not specified, add \"front view, facing the camera\" as a safe " +
        "default. Add professional lighting and quality terminology appropriate to the description. " +
        "Return ONLY the enhanced prompt text on a single line, with no explanation, no quotes, and " +
        "no preamble.\n\nRough description: $roughPrompt"

/**
 * Executor for [promptEnhanceLlmSpec], built from an injectable [transport] so common tests can
 * drive it without a live Ollama server — same pattern as [ollamaGenerateExecutor].
 */
public fun promptEnhanceLlmExecutor(
    transport: suspend (url: String, body: String) -> String = ::ollamaHttpPost,
): NodeExecutor = NodeExecutor { inputs ->
    val roughPrompt = inputs.stringOr("prompt", "")
    val negativePrompt = inputs.stringOr("negative_prompt", "blurry, low quality, cropped, watermark")
    val model = inputs.stringOr("model", "").ifBlank { null }
    val host = inputs.stringOr("host", "").ifBlank { null }
    val enhanced = if (roughPrompt.isBlank()) null else runCatching {
        transport(ollamaGenerateUrl(host), ollamaGenerateBody(promptEnhanceLlmInstruction(roughPrompt), model ?: "llama3.1"))
    }.mapCatching { parseOllamaResponse(it) }.getOrNull()?.trim()?.takeIf(String::isNotBlank)
    mapOf(
        "prompt" to WorkflowValue.StringValue(enhanced ?: roughPrompt),
        "negative_prompt" to WorkflowValue.StringValue(negativePrompt),
        "enhanced" to WorkflowValue.BooleanValue(enhanced != null),
    )
}

/** Default production executor for [ShortsNodeTypes.PROMPT_ENHANCE_LLM]. */
public val promptEnhanceLlmExecutor: NodeExecutor = promptEnhanceLlmExecutor()
