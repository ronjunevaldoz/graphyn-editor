package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleOr
import com.ronjunevaldoz.graphyn.core.model.intOr
import com.ronjunevaldoz.graphyn.core.model.stringOr

/** Extracts a single top-level string field (niche/visual_style/narration) from a validated
 * comparison-arc record. Sibling to [storyboardFieldSpec] — same shape, different record. */
public val comparisonFieldSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_FIELD, label = "Comparison Field", category = ShortsConstants.CATEGORY,
    description = "Extracts a single string field from a validated comparison-arc record.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false), PortSpec("field", WorkflowType.StringType)),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)

/** Executor for [comparisonFieldSpec]. */
public val comparisonFieldExecutor: NodeExecutor = NodeExecutor { inputs ->
    val field = inputs.stringOr("field", "")
    val record = (inputs["input"] as? WorkflowValue.RecordValue)?.fields
    val value = (record?.get(field) as? WorkflowValue.StringValue)?.value.orEmpty()
    mapOf("result" to WorkflowValue.StringValue(value))
}

/** Extracts one field of one pair (by index) from a validated comparison-arc record. */
public val comparisonPairFieldSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_PAIR_FIELD, label = "Comparison Pair Field", category = ShortsConstants.CATEGORY,
    description = "Extracts one field of one pair (by index) from a validated comparison-arc record.",
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
        PortSpec("index", WorkflowType.IntType),
        PortSpec("field", WorkflowType.StringType),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)

/** Executor for [comparisonPairFieldSpec]. */
public val comparisonPairFieldExecutor: NodeExecutor = NodeExecutor { inputs ->
    val index = (inputs["index"] as? WorkflowValue.IntValue)?.value ?: 0
    val field = inputs.stringOr("field", "")
    val pairs = (inputs["input"] as? WorkflowValue.RecordValue)?.fields?.get("pairs") as? WorkflowValue.ListValue
    val pair = pairs?.items?.getOrNull(index) as? WorkflowValue.RecordValue
    val value = (pair?.fields?.get(field) as? WorkflowValue.StringValue)?.value.orEmpty()
    mapOf("result" to WorkflowValue.StringValue(value))
}

/**
 * Builds caption records (text/start_ms/end_ms) from a validated comparison-arc's pairs — each
 * pair contributes two beats (question then answer) instead of storyboard's one caption per scene.
 */
public val comparisonCaptionsSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_CAPTIONS, label = "Comparison Captions", category = ShortsConstants.CATEGORY,
    description = "Builds caption records (text/start_ms/end_ms) from a validated comparison-arc's pairs.",
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
        PortSpec("pair_duration_ms", WorkflowType.DoubleType),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType)),
)

/** Executor for [comparisonCaptionsSpec]. Splits each pair's duration in half: question beat then answer beat. */
public val comparisonCaptionsExecutor: NodeExecutor = NodeExecutor { inputs ->
    val pairDurationMs = (inputs["pair_duration_ms"] as? WorkflowValue.DoubleValue)?.value ?: 4000.0
    val beatDurationMs = pairDurationMs / 2.0
    val pairs = (inputs["input"] as? WorkflowValue.RecordValue)?.fields?.get("pairs") as? WorkflowValue.ListValue
    val captions = pairs?.items?.flatMapIndexed { index, pair ->
        val fields = (pair as? WorkflowValue.RecordValue)?.fields
        val question = (fields?.get("question") as? WorkflowValue.StringValue)?.value.orEmpty()
        val answer = (fields?.get("answer") as? WorkflowValue.StringValue)?.value.orEmpty()
        val pairStart = index * pairDurationMs
        listOf(
            WorkflowValue.RecordValue(
                mapOf(
                    "text" to WorkflowValue.StringValue(question),
                    "start_ms" to WorkflowValue.DoubleValue(pairStart),
                    "end_ms" to WorkflowValue.DoubleValue(pairStart + beatDurationMs),
                ),
            ),
            WorkflowValue.RecordValue(
                mapOf(
                    "text" to WorkflowValue.StringValue(answer),
                    "start_ms" to WorkflowValue.DoubleValue(pairStart + beatDurationMs),
                    "end_ms" to WorkflowValue.DoubleValue(pairStart + pairDurationMs),
                ),
            ),
        )
    }.orEmpty()
    mapOf("result" to WorkflowValue.ListValue(captions))
}

/**
 * Divides a measured narration audio duration evenly across [COMPARISON_PAIR_COUNT] pairs, so each
 * pair's Ken Burns clip (and the matching caption beats) actually spans as long as the narration
 * takes to say it, instead of a fixed guess with no relationship to the real audio length — that
 * mismatch was the root cause of narration/caption timing drifting further apart every pair.
 * Narration is one continuous TTS render (not per-pair segments, per [buildComparisonPrompt]'s
 * "one flowing script" instruction), so even division across pairs is the best approximation
 * available without per-sentence timestamps from the TTS engine.
 *
 * Clamped to a max of 8000ms/pair (32s total) as defense-in-depth: `media.text_to_speech.qwen3`'s
 * reported `duration_ms` was previously observed wildly overshooting real speech length for
 * multi-sentence narration (a 492-character script measured at ~164s instead of the expected
 * ~35s). Root cause: Qwen3TTS's codec generation loop only stops on a sampled EOS token, and a
 * single generate() call spanning more than one sentence made EOS unreliable, running to the
 * max_audio_tokens ceiling instead. Fixed at the source by chunking narration into one
 * synthesize() call per sentence (`Qwen3TtsCli.kt`/`Qwen3TextChunking.kt` in the Graphyn engine
 * repo) — `duration_ms` now tracks real speech length again. This clamp stays in place as a
 * safety net against any future synthesis-layer regression, not as the fix itself.
 */
public val comparisonPairDurationSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_PAIR_DURATION, label = "Comparison Pair Duration", category = ShortsConstants.CATEGORY,
    description = "Divides a measured narration duration evenly across the comparison pairs.",
    inputs = listOf(
        PortSpec("narration_duration_ms", WorkflowType.DoubleType),
        PortSpec("pair_count", WorkflowType.IntType, required = false),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.DoubleType)),
    defaultValues = mapOf("pair_count" to WorkflowValue.IntValue(COMPARISON_PAIR_COUNT)),
)

/** Executor for [comparisonPairDurationSpec]. Floors at 1500ms so a very short narration still
 * leaves each pair's photos legible on screen, and caps at 8000ms so a TTS duration anomaly can't
 * blow up the short into an unwatchably long clip (see the class doc for the confirmed case). */
public val comparisonPairDurationExecutor: NodeExecutor = NodeExecutor { inputs ->
    val narrationDurationMs = inputs.doubleOr("narration_duration_ms", 0.0)
    val pairCount = inputs.intOr("pair_count", COMPARISON_PAIR_COUNT).coerceAtLeast(1)
    val perPairMs = (narrationDurationMs / pairCount).coerceIn(1500.0, 8000.0)
    mapOf("result" to WorkflowValue.DoubleValue(perPairMs))
}
