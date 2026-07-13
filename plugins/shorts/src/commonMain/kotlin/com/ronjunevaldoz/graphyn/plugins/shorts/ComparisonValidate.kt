package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Validates the Ollama comparison-arc JSON and force-unloads the Ollama model before returning —
 * sibling to [storyboardValidateExecutor], same salvage/fallback discipline, different shape
 * (paired comparisons, not a flat scene list). See that executor's doc comment for why this is
 * compiled Kotlin instead of a `script.eval` .kts script, and why top-level fields are NOT
 * salvaged (throws instead) while per-pair fields are patched rather than dropped.
 *
 * "narration" is not requested from the model at all — [buildComparisonPrompt]'s doc comment
 * explains why — and is instead built here, deterministically, from the validated pairs' own
 * question/answer fields. Confirmed necessary via direct audio/caption sync inspection: an earlier
 * version that trusted the model's own free-form narration prose produced a real response covering
 * 7 spoken comparisons against only 4 structured pairs (one of which was itself blank) — the audio
 * had no reliable correspondence to which pair was on screen. Rebuilding narration as one "question
 * answer" segment per pair, in pair order, guarantees the spoken content and the visual pairs
 * describe the same things in the same order — [comparisonPairDurationExecutor]'s
 * even-division-per-pair timing assumption only holds when this is true.
 */
public val comparisonValidateExecutor: NodeExecutor = NodeExecutor { inputs ->
    runCatching { unloadOllamaModel() }

    val raw = (inputs["input"] as? WorkflowValue.RecordValue)?.fields
    val niche = (raw?.get("niche") as? WorkflowValue.StringValue)?.value
    val visualStyle = (raw?.get("visual_style") as? WorkflowValue.StringValue)?.value
    val rawPairs = (raw?.get("pairs") as? WorkflowValue.ListValue)?.items

    val missing = buildList {
        if (niche == null) add("niche")
        if (visualStyle == null) add("visual_style")
        if (rawPairs == null) add("pairs")
    }
    check(missing.isEmpty()) {
        val diagnostics = (inputs["diagnostics"] as? WorkflowValue.StringValue)?.value ?: "no chain diagnostics wired"
        "Ollama comparison-arc response is missing top-level field(s): ${missing.joinToString()}. " +
            "Check GRAPHYN_OLLAMA_HOST is reachable and the model is returning valid JSON. " +
            "Chain: $diagnostics. Raw response: $raw"
    }

    // A blank string (Ollama returned the key with an empty value) must salvage the same as a
    // missing key — confirmed necessary: a real response had label_a/label_b missing (correctly
    // salvaged to "Thing A"/"Thing B") but question/answer present as "" (not missing), which the
    // old `?: fallback` on the raw nullable value never caught, leaving a pair with real labels but
    // a silently blank caption and no matching narration content.
    fun Map<String, WorkflowValue>?.stringField(key: String): String? =
        (this?.get(key) as? WorkflowValue.StringValue)?.value?.takeIf(String::isNotBlank)

    val pairs = List(COMPARISON_PAIR_COUNT) { index ->
        val fields = (rawPairs!!.getOrNull(index) as? WorkflowValue.RecordValue)?.fields
        val labelA = fields.stringField("label_a") ?: "Thing A"
        val labelB = fields.stringField("label_b") ?: "Thing B"
        val promptA = fields.stringField("prompt_a")
            ?: listOf(labelA, niche!!).filter(String::isNotBlank).joinToString(", ")
        val promptB = fields.stringField("prompt_b")
            ?: listOf(labelB, niche!!).filter(String::isNotBlank).joinToString(", ")
        val question = fields.stringField("question") ?: "What's the difference?"
        val answer = fields.stringField("answer") ?: "$labelA and $labelB differ."
        ComparisonPair(labelA, labelB, promptA, promptB, question, answer)
    }
    val builtNarration = pairs.joinToString(" ") { "${it.question} ${it.answer}" }
    val result = WorkflowValue.RecordValue(
        mapOf(
            "niche" to WorkflowValue.StringValue(niche!!),
            "visual_style" to WorkflowValue.StringValue(visualStyle!!),
            "narration" to WorkflowValue.StringValue(builtNarration),
            "pairs" to WorkflowValue.ListValue(pairs.map { it.toWorkflowValue() }),
        ),
    )
    mapOf("value" to result)
}

private data class ComparisonPair(
    val labelA: String,
    val labelB: String,
    val promptA: String,
    val promptB: String,
    val question: String,
    val answer: String,
) {
    fun toWorkflowValue(): WorkflowValue.RecordValue = WorkflowValue.RecordValue(
        mapOf(
            "label_a" to WorkflowValue.StringValue(labelA),
            "label_b" to WorkflowValue.StringValue(labelB),
            "prompt_a" to WorkflowValue.StringValue(promptA),
            "prompt_b" to WorkflowValue.StringValue(promptB),
            "question" to WorkflowValue.StringValue(question),
            "answer" to WorkflowValue.StringValue(answer),
        ),
    )
}
