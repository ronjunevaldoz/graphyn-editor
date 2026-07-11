package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.stringValue as s
import com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION

/** Fixed at 2: one base mascot reference, edited once via FLUX Kontext into a left-pointing pose
 * ([MascotPointDirections.POINT_LEFT]), then mirrored via [mascotFlipSubgraph] into the right-
 * pointing pose instead of a second independent Kontext edit — see that function's doc comment for
 * why (unreliable left/right prompting, and a second sample can still drift the character). */
private const val COMPARISON_MASCOT_DIRECTION_COUNT = 2

internal fun comparisonShortGuideNote() = guideNote(
    """
    Comparison Short · "X vs Y — what's the difference?"

    An Ollama-generated comparison arc (niche, visual style, narration,
    per-pair labels+prompts+question+answer) drives every pair. Each
    pair composites two original AI-generated images with one recurring
    mascot (one base image, Kontext-edited into left/right-pointing
    variants) into one frame — original content only, not a copy of any
    specific reference video. Each pair's clip length matches the real
    narration audio duration.
    """,
)

internal fun comparisonShortNodes(
    topic: String,
    width: Int,
    height: Int,
    mascotDescription: String = DEFAULT_MASCOT_DESCRIPTION,
    useKenBurns: Boolean,
) = buildList {
    add(NodeRef("comparison", STORYBOARD_SUBGRAPH_NODE_TYPE, subgraph = comparisonGeneratorSubgraph(topic)))
    // Extracted ONCE here and fanned out to every pair, instead of each comparisonPairSubgraph
    // instance re-extracting the same value 4 times (niche/visualStyle don't vary per pair, unlike
    // labelA/labelB/promptA/promptB — see comparisonPairSubgraph's doc comment).
    add(NodeRef("niche", COMPARISON_FIELD_NODE_TYPE, config = mapOf("field" to s("niche"))))
    add(NodeRef("visualStyle", COMPARISON_FIELD_NODE_TYPE, config = mapOf("field" to s("visual_style"))))
    add(NodeRef("narration", COMPARISON_FIELD_NODE_TYPE, config = mapOf("field" to s("narration"))))
    add(NodeRef("captionsScript", COMPARISON_CAPTIONS_NODE_TYPE))
    add(NodeRef("narrate", "media.text_to_speech.say", config = mapOf("voice_id" to s("Samantha"), "speed" to d(1.0))))
    add(NodeRef("pairDuration", COMPARISON_PAIR_DURATION_NODE_TYPE))
    addAll(comparisonMascotNodes(width, height, mascotDescription))
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
    add(ConnectionRef("comparison", "value", "niche", "input"))
    add(ConnectionRef("comparison", "value", "visualStyle", "input"))
    add(ConnectionRef("comparison", "value", "narration", "input"))
    add(ConnectionRef("comparison", "value", "captionsScript", "input"))
    add(ConnectionRef("narration", "result", "narrate", "text"))
    add(ConnectionRef("narrate", "duration_ms", "pairDuration", "narration_duration_ms"))
    add(ConnectionRef("pairDuration", "result", "captionsScript", "pair_duration_ms"))
    // Base mascot generates once; the left-pointing edit conditions on the base's raw image output
    // (sd.id_cond's ref_images is list-typed, so this single connection auto-wraps into a
    // one-element list — see mascotPointEditSubgraph's doc comment). Gated against the base, same
    // shared-GPU discipline as the pair loop below. The right-pointing "edit" is a pixel mirror of
    // mascotLeft's own output (mascotFlipSubgraph) — its "image" connection is itself the ordering
    // dependency, so no separate gate is needed; it's a fast ffmpeg call, not a competing GPU job.
    add(ConnectionRef("mascotBase", "video", "mascotLeft", "ref_images"))
    add(ConnectionRef("mascotBase", "video", "mascotLeft", "gate"))
    add(ConnectionRef("mascotLeft", "video", "mascotRight", "image"))
    repeat(COMPARISON_PAIR_COUNT) { index ->
        val previous = if (index == 0) "mascotRight" else "pair${index - 1}"
        add(ConnectionRef(previous, "video", "pair$index", "gate"))
        add(ConnectionRef("comparison", "value", "pair$index", "input"))
        add(ConnectionRef("niche", "result", "pair$index", "niche"))
        add(ConnectionRef("visualStyle", "result", "pair$index", "visual_style"))
        // mascotLeft/mascotRight already import their own raw path into a real image handle
        // internally (see mascotPointEditSubgraph) — no separate top-level Import node needed.
        add(ConnectionRef("mascot${directionName(index % COMPARISON_MASCOT_DIRECTION_COUNT)}", "video", "pair$index", "mascot"))
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

private fun comparisonMascotNodes(width: Int, height: Int, mascotDescription: String) = buildList {
    // One base reference — never composited directly, only consumed as ref_images by the edits
    // below, so it does not need its own media.image_import.
    add(NodeRef(
        "mascotBase", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
        subgraph = mascotSubgraph(id = "comparison-mascot-base", mascotDescription = mascotDescription, width = width, height = height),
    ))
    add(NodeRef(
        "mascotLeft", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
        subgraph = mascotPointEditSubgraph(id = "comparison-mascot-Left", editInstruction = MascotPointDirections.POINT_LEFT, width = width, height = height),
    ))
    add(NodeRef(
        "mascotRight", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
        subgraph = mascotFlipSubgraph(id = "comparison-mascot-Right-flip"),
    ))
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
