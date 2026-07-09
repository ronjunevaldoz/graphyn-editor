package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

internal const val COMPARISON_OUTPUT_DIR = "outputs/comparison-short"

private fun comparisonOutput(fileName: String) = "$COMPARISON_OUTPUT_DIR/$fileName"

private val COMPARISON_MASCOT_POSES = listOf(MascotPoses.NEUTRAL, MascotPoses.CONFUSED, MascotPoses.EXPLAINING)

/**
 * Comparison-format short: a generated comparison arc drives four paired image scenes, with one
 * recurring mascot pose per pair and narration timing that comes from the measured audio duration.
 */
internal fun comparisonShortWorkflow(
    topic: String,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    mascotDescription: String = com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION,
    useKenBurns: Boolean = true,
) = WorkflowDefinition(
    id = "comparison-short",
    name = "Comparison Short (X vs Y)",
    nodes = buildList {
        add(guideNote(
            """
            Comparison Short · "X vs Y — what's the difference?"

            An Ollama-generated comparison arc (niche, visual style, narration,
            per-pair labels+prompts+question+answer) drives every pair. Each
            pair composites two original AI-generated images with a recurring
            mascot pose into one frame — original content only, not a copy
            of any specific reference video. Mascot pose cycles per pair and
            each pair's clip length matches the real narration audio duration.
            """,
        ))
        add(NodeRef("comparison", STORYBOARD_SUBGRAPH_NODE_TYPE, subgraph = comparisonGeneratorSubgraph(topic)))
        add(NodeRef("niche", COMPARISON_FIELD_NODE_TYPE, config = mapOf("field" to s("niche"))))
        add(NodeRef("visualStyle", COMPARISON_FIELD_NODE_TYPE, config = mapOf("field" to s("visual_style"))))
        add(NodeRef("narration", COMPARISON_FIELD_NODE_TYPE, config = mapOf("field" to s("narration"))))
        add(NodeRef("captionsScript", COMPARISON_CAPTIONS_NODE_TYPE))
        add(NodeRef("narrate", "media.text_to_speech.say", config = mapOf("voice_id" to s("Samantha"), "speed" to d(1.0))))
        add(NodeRef("pairDuration", COMPARISON_PAIR_DURATION_NODE_TYPE))
        COMPARISON_MASCOT_POSES.forEachIndexed { poseIndex, pose ->
            add(NodeRef(
                "mascot$poseIndex", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotSubgraph(
                    id = "comparison-mascot-$poseIndex", mascotDescription = mascotDescription,
                    poseInstruction = pose, width = width, height = height,
                ),
            ))
            add(NodeRef("mascot${poseIndex}Import", "media.image_import"))
        }
        repeat(COMPARISON_PAIR_COUNT) { index ->
            add(NodeRef(
                "pair$index", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = comparisonPairSubgraph(
                    id = "comparison-pair-$index",
                    pairIndex = index,
                    width = width,
                    height = height,
                    useKenBurns = useKenBurns,
                ),
            ))
            add(NodeRef("pair${index}Save", "media.video_encode", config = mapOf(
                "output_path" to s(comparisonOutput("comparison-short.pair$index.mp4")), "bitrate" to s("medium"), "codec" to s("h264"),
            )))
        }
        add(NodeRef("stitch", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(0)))
        add(NodeRef("stitchSave", "media.video_encode", config = mapOf(
            "output_path" to s(comparisonOutput("comparison-short.stitched.mp4")), "bitrate" to s("medium"), "codec" to s("h264"),
        )))
        add(shortsCaptionStyleNode())
        add(NodeRef("captionOverlay", "media.caption_overlay"))
        add(NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to s(comparisonOutput("comparison-short.mp4")), "bitrate" to s("high"), "codec" to s("h264"),
        )))
        add(NodeRef("output", "media.file_output"))
        add(NodeRef("comparisonMetadata", "demo.comparison.metadata"))
        add(NodeRef("comparisonJson", "json.stringify", config = mapOf("pretty" to WorkflowValue.BooleanValue(true))))
        add(NodeRef("comparisonJsonWrite", "io.file_write", config = mapOf(
            "path" to s(comparisonOutput("comparison-short.comparison.json")), "append" to WorkflowValue.BooleanValue(false),
        )))
    },
    connections = buildList {
        add(ConnectionRef("comparison", "value", "niche", "input"))
        add(ConnectionRef("comparison", "value", "visualStyle", "input"))
        add(ConnectionRef("comparison", "value", "narration", "input"))
        add(ConnectionRef("comparison", "value", "captionsScript", "input"))

        add(ConnectionRef("narration", "result", "narrate", "text"))
        add(ConnectionRef("narrate", "duration_ms", "pairDuration", "narration_duration_ms"))
        add(ConnectionRef("pairDuration", "result", "captionsScript", "pair_duration_ms"))

        COMPARISON_MASCOT_POSES.indices.forEach { poseIndex ->
            add(ConnectionRef("mascot$poseIndex", "video", "mascot${poseIndex}Import", "path"))
            if (poseIndex > 0) {
                add(ConnectionRef("mascot${poseIndex - 1}", "video", "mascot$poseIndex", "gate"))
            }
        }
        val lastMascotIndex = COMPARISON_MASCOT_POSES.lastIndex

        repeat(COMPARISON_PAIR_COUNT) { index ->
            if (index == 0) {
                add(ConnectionRef("mascot$lastMascotIndex", "video", "pair$index", "gate"))
            } else {
                add(ConnectionRef("pair${index - 1}", "video", "pair$index", "gate"))
            }
            add(ConnectionRef("comparison", "value", "pair$index", "input"))
            add(ConnectionRef("mascot${index % COMPARISON_MASCOT_POSES.size}Import", "image", "pair$index", "mascot"))
            add(ConnectionRef("pairDuration", "result", "pair$index", "duration_ms"))
            add(ConnectionRef("pair$index", "video", "stitch", "video${index + 1}"))
            add(ConnectionRef("pair$index", "video", "pair${index}Save", "video"))
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
    },
)
