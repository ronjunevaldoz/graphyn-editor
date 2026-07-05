package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val STORYBOARD_VALIDATE_NODE_TYPE = "demo.storyboard.validate"

private val FALLBACK_STORYBOARD = WorkflowValue.RecordValue(
    mapOf(
        "niche" to WorkflowValue.StringValue("cooking"),
        "visual_style" to WorkflowValue.StringValue("warm cinematic photography, shallow depth of field"),
        "character" to WorkflowValue.StringValue("a young chef with curly brown hair, wearing a white apron"),
        "narration" to WorkflowValue.StringValue(
            "This is how a five-star dish comes together. Fresh ramen, made from scratch. And that's how it's served.",
        ),
        "scenes" to WorkflowValue.ListValue(
            listOf(
                "a chef plating a dish in a modern kitchen" to "This is how it comes together.",
                "steam rising from a fresh bowl of ramen, close-up" to "Fresh, made from scratch.",
                "a plated dish served at a restaurant table, warm lighting" to "And that's how it's served.",
            ).map { (prompt, caption) ->
                WorkflowValue.RecordValue(
                    mapOf("prompt" to WorkflowValue.StringValue(prompt), "caption" to WorkflowValue.StringValue(caption)),
                )
            },
        ),
    ),
)

/**
 * Validates the Ollama storyboard JSON and force-unloads the Ollama model before returning — both as
 * compiled Kotlin instead of a `script.eval` .kts script. The scripted version reliably crashed
 * Kotlin's JSR-223 IR backend ("Exception during psi2ir") on this validation logic's shape once
 * chained after the other scripts in [storyboardGeneratorSubgraph]; a real executor has no such
 * runtime-compilation step to crash. The old Ollama-driven pipeline (imageShortsWorkflow) trusted the
 * LLM's JSON shape blindly and broke downstream — this falls back to a fixed, known-good storyboard
 * on any shape mismatch instead.
 *
 * Scene-level salvage: a small local model under grammar-constrained JSON mode occasionally drops a
 * field on exactly one scene (confirmed: `qwen3:8b` once omitted `prompt` on scene 3 only, everything
 * else — niche/visual_style/character/narration/the other 2 scenes — was fine). Discarding the whole
 * storyboard for one bad scene threw away a correct, on-topic result in favor of the unrelated hardcoded
 * fallback. Only the top-level identity fields (niche/visual_style/narration) trigger a full fallback;
 * a malformed scene is patched from niche/character instead of dropped, so scene count stays fixed.
 */
internal val storyboardValidateExecutor = NodeExecutor { inputs ->
    runCatching { unloadOllamaModel() }

    val raw = (inputs["input"] as? WorkflowValue.RecordValue)?.fields
    val niche = (raw?.get("niche") as? WorkflowValue.StringValue)?.value
    val visualStyle = (raw?.get("visual_style") as? WorkflowValue.StringValue)?.value
    // character is optional — some models omit it for topics with no recurring subject, and that's fine.
    val character = (raw?.get("character") as? WorkflowValue.StringValue)?.value.orEmpty()
    val narration = (raw?.get("narration") as? WorkflowValue.StringValue)?.value
    val rawScenes = (raw?.get("scenes") as? WorkflowValue.ListValue)?.items

    val result = if (niche == null || visualStyle == null || narration == null || rawScenes == null) {
        FALLBACK_STORYBOARD
    } else {
        val scenes = List(STORYBOARD_SCENE_COUNT) { index ->
            val fields = (rawScenes.getOrNull(index) as? WorkflowValue.RecordValue)?.fields
            val caption = (fields?.get("caption") as? WorkflowValue.StringValue)?.value
            val prompt = (fields?.get("prompt") as? WorkflowValue.StringValue)?.value
                ?: listOf(character, niche).filter(String::isNotBlank).joinToString(", ").ifBlank { niche }
            prompt to (caption ?: prompt)
        }
        WorkflowValue.RecordValue(
            mapOf(
                "niche" to WorkflowValue.StringValue(niche),
                "visual_style" to WorkflowValue.StringValue(visualStyle),
                "character" to WorkflowValue.StringValue(character),
                "narration" to WorkflowValue.StringValue(narration),
                "scenes" to WorkflowValue.ListValue(
                    scenes.map { (prompt, caption) ->
                        WorkflowValue.RecordValue(
                            mapOf("prompt" to WorkflowValue.StringValue(prompt), "caption" to WorkflowValue.StringValue(caption)),
                        )
                    },
                ),
            ),
        )
    }
    mapOf("value" to result)
}
