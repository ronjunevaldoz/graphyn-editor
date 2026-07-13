package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

/** Fixed at 2: a left-pointing and a right-pointing mascot image, supplied by the caller (see
 * [comparisonShortWorkflow]'s doc comment) rather than generated in-workflow. */
private const val COMPARISON_MASCOT_DIRECTION_COUNT = 2

internal fun comparisonShortGuideNote() = guideNote(
    """
    Comparison Short · "X vs Y — what's the difference?"

    An Ollama-generated comparison arc (niche, visual style, narration,
    per-pair labels+prompts+question+answer) drives every pair. Each
    pair composites two original AI-generated images with one recurring
    mascot — bring your own left-pointing/right-pointing images (local SD,
    ChatGPT, Gemini, manual art) via mascotLeftImagePath/mascotRightImagePath
    — into one frame. Each pair's clip length matches the real narration
    audio duration.
    """,
)

internal fun comparisonShortNodes(
    topic: String,
    width: Int,
    height: Int,
    mascotLeftImagePath: String,
    mascotRightImagePath: String,
    useKenBurns: Boolean,
) = buildList {
    add(NodeRef("comparison", STORYBOARD_SUBGRAPH_NODE_TYPE, subgraph = comparisonGeneratorSubgraph(topic)))
    // Extracted ONCE here and fanned out to every pair, instead of each comparisonPairSubgraph
    // instance re-extracting the same value 4 times (niche/visualStyle don't vary per pair, unlike
    // labelA/labelB/promptA/promptB — see comparisonPairSubgraph's doc comment).
    add(NodeRef("fields", COMPARISON_FIELDS_NODE_TYPE))
    add(NodeRef("captionsScript", COMPARISON_CAPTIONS_NODE_TYPE))
    add(NodeRef("narrate", "media.text_to_speech.say", config = mapOf("voice_id" to s("Samantha"), "speed" to d(1.0))))
    add(NodeRef("pairDuration", COMPARISON_PAIR_DURATION_NODE_TYPE))
    addAll(comparisonMascotNodes(mascotLeftImagePath, mascotRightImagePath))
    addAll(comparisonPairNodes(width, height, useKenBurns))
    add(NodeRef("stitch", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(0)))
    add(NodeRef("stitchSave", "media.video_encode", config = mapOf("output_path" to s(comparisonOutput("comparison-short.stitched.mp4")), "bitrate" to s("medium"), "codec" to s("h264"))))
    // Center vertically: the composited frame's photos+labels occupy the top ~40% and the mascot
    // sits near the bottom, leaving an empty band in the middle that MiddleCenter lands in cleanly
    // — BottomCenter (the shared shorts default) crowded the mascot instead.
    add(shortsCaptionStyleNode(overrides = mapOf("alignment" to s("MiddleCenter"))))
    add(NodeRef("captionOverlay", "media.caption_overlay"))
    add(NodeRef("encode", "media.video_encode", config = mapOf("output_path" to s(comparisonOutput("comparison-short.mp4")), "bitrate" to s("high"), "codec" to s("h264"))))
    add(NodeRef("output", "media.file_output"))
    add(NodeRef("comparisonMetadata", "demo.comparison.metadata"))
    add(NodeRef("comparisonJson", "json.stringify", config = mapOf("pretty" to WorkflowValue.BooleanValue(true))))
    add(NodeRef("comparisonJsonWrite", "io.file_write", config = mapOf("path" to s(comparisonOutput("comparison-short.comparison.json")), "append" to WorkflowValue.BooleanValue(false))))
}

internal fun comparisonShortConnections() = buildList {
    add(ConnectionRef("comparison", "value", "fields", "input"))
    add(ConnectionRef("comparison", "value", "captionsScript", "input"))
    add(ConnectionRef("fields", "narration", "narrate", "text"))
    add(ConnectionRef("narrate", "duration_ms", "pairDuration", "narration_duration_ms"))
    add(ConnectionRef("pairDuration", "result", "captionsScript", "pair_duration_ms"))
    repeat(COMPARISON_PAIR_COUNT) { index ->
        // mascotLeft/mascotRight are now plain media.image_import nodes (caller-supplied paths, no
        // generation) — importing a file is near-instant and doesn't contend for GPU, but pair0's
        // "gate" port is still required, so it's satisfied off mascotLeft rather than left unwired.
        val previous = if (index == 0) "mascotLeft" else "pair${index - 1}"
        val previousPort = if (index == 0) "image" else "video"
        add(ConnectionRef(previous, previousPort, "pair$index", "gate"))
        add(ConnectionRef("comparison", "value", "pair$index", "input"))
        add(ConnectionRef("fields", "niche", "pair$index", "niche"))
        add(ConnectionRef("fields", "visual_style", "pair$index", "visual_style"))
        add(ConnectionRef("mascot${directionName(index % COMPARISON_MASCOT_DIRECTION_COUNT)}", "image", "pair$index", "mascot"))
        add(ConnectionRef("pairDuration", "result", "pair$index", "duration_ms"))
        add(ConnectionRef("pair$index", "video", "stitch", "video${index + 1}"))
    }
    add(ConnectionRef("stitch", "video", "captionOverlay", "video"))
    add(ConnectionRef("stitch", "video", "stitchSave", "video"))
    add(ConnectionRef("captionsScript", "result", "captionOverlay", "captions"))
    add(ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"))
    add(ConnectionRef("captionOverlay", "video", "encode", "video"))
    add(ConnectionRef("narrate", "audio", "encode", "audio"))
    add(ConnectionRef("encode", "file_path", "output", "file_path"))
    add(ConnectionRef("comparison", "value", "comparisonMetadata", "input"))
    add(ConnectionRef("comparisonMetadata", "value", "comparisonJson", "value"))
    add(ConnectionRef("comparisonJson", "text", "comparisonJsonWrite", "content"))
}

/** "Left"/"Right" by direction index — used for both node ids and connection wiring so they stay
 * in sync (avoids a numeric-index naming scheme that reads worse for a fixed 2-direction set). */
internal fun directionName(index: Int) = if (index == 0) "Left" else "Right"

private fun comparisonMascotNodes(mascotLeftImagePath: String, mascotRightImagePath: String) = buildList {
    add(NodeRef("mascotLeft", "media.image_import", config = mapOf("path" to s(mascotLeftImagePath))))
    add(NodeRef("mascotRight", "media.image_import", config = mapOf("path" to s(mascotRightImagePath))))
}

private fun comparisonPairNodes(width: Int, height: Int, useKenBurns: Boolean) =
    (0 until COMPARISON_PAIR_COUNT).map { index ->
        NodeRef(
            "pair$index",
            SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
            subgraph = comparisonPairSubgraph(
                id = "comparison-pair-$index",
                pairIndex = index,
                width = width,
                height = height,
                useKenBurns = useKenBurns,
                outputPath = comparisonOutput("comparison-short.pair$index.mp4"),
            ),
        )
    }
