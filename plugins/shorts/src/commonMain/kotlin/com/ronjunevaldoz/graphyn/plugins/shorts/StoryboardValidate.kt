package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Validates the Ollama storyboard JSON and force-unloads the Ollama model before returning — both as
 * compiled Kotlin instead of a `script.eval` .kts script. The scripted version reliably crashed
 * Kotlin's JSR-223 IR backend ("Exception during psi2ir") on this validation logic's shape once
 * chained after the other scripts in [storyboardGeneratorSubgraph]; a real executor has no such
 * runtime-compilation step to crash.
 *
 * Scene-level salvage: a small local model under grammar-constrained JSON mode occasionally drops a
 * field on exactly one scene (confirmed: `qwen3:8b` once omitted `prompt` on scene 3 only, everything
 * else — niche/visual_style/character/narration/the other 2 scenes — was fine). A malformed scene is
 * patched from niche/character instead of dropped, so scene count stays fixed.
 *
 * Top-level fields (niche/visual_style/narration/scenes) are NOT salvaged — a missing top-level field
 * usually means Ollama was unreachable or returned something the parser can't use at all, and this
 * validation runs before the expensive Flux/TTS steps. Changed 2026-07-08: this used to silently
 * substitute a hardcoded "chef making ramen" storyboard instead of throwing — confirmed harmful in
 * practice (an unreachable Ollama host silently produced 3 unrelated, fully-generated shorts with no
 * visible error; `Success=28` gave no indication anything was wrong). Throwing here is strictly
 * cheaper than continuing, since it fails before any GPU spend.
 */
public val storyboardValidateExecutor: NodeExecutor = NodeExecutor { inputs ->
    runCatching { unloadOllamaModel() }

    val raw = (inputs["input"] as? WorkflowValue.RecordValue)?.fields
    val niche = (raw?.get("niche") as? WorkflowValue.StringValue)?.value
    val visualStyle = (raw?.get("visual_style") as? WorkflowValue.StringValue)?.value
    // character is optional — some models omit it for topics with no recurring subject, and that's fine.
    val character = (raw?.get("character") as? WorkflowValue.StringValue)?.value.orEmpty()
    val narration = (raw?.get("narration") as? WorkflowValue.StringValue)?.value
    val rawScenes = (raw?.get("scenes") as? WorkflowValue.ListValue)?.items

    val missing = buildList {
        if (niche == null) add("niche")
        if (visualStyle == null) add("visual_style")
        if (narration == null) add("narration")
        if (rawScenes == null) add("scenes")
    }
    check(missing.isEmpty()) {
        "Ollama storyboard response is missing top-level field(s): ${missing.joinToString()}. " +
            "Check GRAPHYN_OLLAMA_HOST is reachable and the model is returning valid JSON. " +
            "Chain: ${ollamaChainDiagnostics(inputs)}. Raw response: $raw"
    }

    val scenes = List(STORYBOARD_SCENE_COUNT) { index ->
        val fields = (rawScenes!!.getOrNull(index) as? WorkflowValue.RecordValue)?.fields
        val caption = (fields?.get("caption") as? WorkflowValue.StringValue)?.value
        val prompt = (fields?.get("prompt") as? WorkflowValue.StringValue)?.value
            ?: listOf(character, niche!!).filter(String::isNotBlank).joinToString(", ").ifBlank { niche }
        prompt to (caption ?: prompt)
    }
    val result = WorkflowValue.RecordValue(
        mapOf(
            "niche" to WorkflowValue.StringValue(niche!!),
            "visual_style" to WorkflowValue.StringValue(visualStyle!!),
            "character" to WorkflowValue.StringValue(character),
            "narration" to WorkflowValue.StringValue(narration!!),
            "scenes" to WorkflowValue.ListValue(
                scenes.map { (prompt, caption) ->
                    WorkflowValue.RecordValue(
                        mapOf("prompt" to WorkflowValue.StringValue(prompt), "caption" to WorkflowValue.StringValue(caption)),
                    )
                },
            ),
        ),
    )
    mapOf("value" to result)
}
