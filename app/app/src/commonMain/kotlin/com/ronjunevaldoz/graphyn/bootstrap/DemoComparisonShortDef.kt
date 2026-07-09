package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

internal const val COMPARISON_OUTPUT_DIR = "outputs/comparison-short"

private fun comparisonOutput(fileName: String) = "$COMPARISON_OUTPUT_DIR/$fileName"

/** Cycled by pair index so the mascot's appearance actually changes across the short instead of
 * being one static pose held the whole video. */
private val COMPARISON_MASCOT_POSES = listOf(MascotPoses.NEUTRAL, MascotPoses.CONFUSED, MascotPoses.EXPLAINING)

/**
 * Comparison-format short: an Ollama-generated comparison arc (niche, shared visual style,
 * narration, "X vs Y" pairs) drives [COMPARISON_PAIR_COUNT] pairs, each rendering two original
 * AI-generated images composited with a recurring mascot pose into one frame, Ken-Burns'd into a
 * clip. Sibling to [imageMotionStoryboardShortWorkflow] — same shape (storyboard → per-item loop,
 * gated for GPU serialization → stitch → captions → narration → encode), different content shape
 * (paired comparisons instead of a flat scene list) and a new compositing step neither
 * [imageMotionSceneSubgraphDynamic] nor any other existing scene type has.
 *
 * The mascot is generated ONCE PER POSE (not once per pair — [COMPARISON_MASCOT_POSES] cycles
 * [COMPARISON_PAIR_COUNT] pairs across 3 distinct poses via `index % 3`) before the per-pair loop,
 * mirroring [characterSheetSubgraphDynamic]'s "reference generated once, referenced many times"
 * pattern, just with 3 references instead of 1 so the mascot's expression actually varies.
 *
 * Each pair's clip length is driven by the real measured narration audio duration (divided evenly
 * across pairs via [COMPARISON_PAIR_DURATION_NODE_TYPE]), not a fixed guess — see that node's doc
 * comment for why. `narrate` therefore runs on its own branch in parallel with the (much slower)
 * per-pair FLUX generation, and both converge at `captionsScript`/each pair's Ken Burns clip.
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
        // Use the OS voice for a stable rerun while the dedicated Oute adapter env is incomplete.
        add(NodeRef("narrate", "media.text_to_speech.say", config = mapOf("voice_id" to s("Samantha"), "speed" to d(1.0))))
        add(NodeRef("pairDuration", COMPARISON_PAIR_DURATION_NODE_TYPE))
        // Generated once per pose (not per pair) — each pair references the pose for `index % 3`
        // via its own image_import below.
        COMPARISON_MASCOT_POSES.forEachIndexed { poseIndex, pose ->
            add(NodeRef(
                "mascot$poseIndex", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotSubgraph(
                    id = "comparison-mascot-$poseIndex", mascotDescription = mascotDescription,
                    poseInstruction = pose, width = width, height = height,
                ),
            ))
            // No separate "save" step for the mascot: media.video_encode is for muxing an actual
            // video container, not persisting a still image — the raw PNG already lands in
            // HttpStableDiffusionBackend's own artifact directory as a side effect of generation.
            // media.image_import below is what turns the raw path into a usable image handle.
            add(NodeRef("mascot${poseIndex}Import", "media.image_import"))
        }
        repeat(COMPARISON_PAIR_COUNT) { index ->
            add(NodeRef("pair${index}LabelA", COMPARISON_PAIR_FIELD_NODE_TYPE, config = mapOf("index" to i(index), "field" to s("label_a"))))
            add(NodeRef("pair${index}LabelB", COMPARISON_PAIR_FIELD_NODE_TYPE, config = mapOf("index" to i(index), "field" to s("label_b"))))
            add(NodeRef("pair${index}PromptA", COMPARISON_PAIR_FIELD_NODE_TYPE, config = mapOf("index" to i(index), "field" to s("prompt_a"))))
            add(NodeRef("pair${index}PromptB", COMPARISON_PAIR_FIELD_NODE_TYPE, config = mapOf("index" to i(index), "field" to s("prompt_b"))))
            add(NodeRef(
                "pair${index}ImageA", STORYBOARD_SUBGRAPH_NODE_TYPE,
                subgraph = comparisonImageSubgraph(id = "comparison-pair-$index-a", width = width, height = height),
            ))
            add(NodeRef(
                "pair${index}ImageB", STORYBOARD_SUBGRAPH_NODE_TYPE,
                subgraph = comparisonImageSubgraph(id = "comparison-pair-$index-b", width = width, height = height),
            ))
            // comparisonImageSubgraph's free output is a raw path string (same shape as the mascot's
            // txt2img output) — media.comparison_layout's image_a/image_b ports need a real
            // MediaTypes image handle, so route through media.image_import first, same as mascotImport.
            add(NodeRef("pair${index}ImageAImport", "media.image_import"))
            add(NodeRef("pair${index}ImageBImport", "media.image_import"))
            add(NodeRef(
                "pair$index", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = comparisonLayoutMotionSubgraph(
                    id = "comparison-pair-$index-layout", width = width, height = height, useKenBurns = useKenBurns,
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

        // Runs in parallel with mascot/pair generation (local TTS, no shared-GPU contention) — its
        // measured duration drives pairDuration, which both captionsScript and every pair's Ken
        // Burns clip read from, so caption timing and clip length both track the real audio length.
        add(ConnectionRef("narration", "result", "narrate", "text"))
        add(ConnectionRef("narrate", "duration_ms", "pairDuration", "narration_duration_ms"))
        add(ConnectionRef("pairDuration", "result", "captionsScript", "pair_duration_ms"))

        // Mascot poses are gated sequentially against each other (same shared GPU discipline as the
        // pair loop below), then the last pose gates pair0's image generation.
        COMPARISON_MASCOT_POSES.indices.forEach { poseIndex ->
            add(ConnectionRef("mascot$poseIndex", "video", "mascot${poseIndex}Import", "path"))
            if (poseIndex > 0) {
                add(ConnectionRef("mascot${poseIndex - 1}", "video", "mascot$poseIndex", "gate"))
            }
        }
        val lastMascotIndex = COMPARISON_MASCOT_POSES.lastIndex

        repeat(COMPARISON_PAIR_COUNT) { index ->
            add(ConnectionRef("comparison", "value", "pair${index}LabelA", "input"))
            add(ConnectionRef("comparison", "value", "pair${index}LabelB", "input"))
            add(ConnectionRef("comparison", "value", "pair${index}PromptA", "input"))
            add(ConnectionRef("comparison", "value", "pair${index}PromptB", "input"))

            add(ConnectionRef("pair${index}PromptA", "result", "pair${index}ImageA", "prompt"))
            add(ConnectionRef("niche", "result", "pair${index}ImageA", "niche"))
            add(ConnectionRef("visualStyle", "result", "pair${index}ImageA", "visual_style"))
            add(ConnectionRef("pair${index}PromptB", "result", "pair${index}ImageB", "prompt"))
            add(ConnectionRef("niche", "result", "pair${index}ImageB", "niche"))
            add(ConnectionRef("visualStyle", "result", "pair${index}ImageB", "visual_style"))

            // Serializes generation exactly like the storyboard pipeline's scene loop — one shared
            // 12GB-class GPU, avoid firing concurrent requests at it.
            if (index == 0) {
                add(ConnectionRef("mascot$lastMascotIndex", "video", "pair${index}ImageA", "gate"))
            } else {
                add(ConnectionRef("pair${index - 1}", "video", "pair${index}ImageA", "gate"))
            }
            add(ConnectionRef("pair${index}ImageA", "value", "pair${index}ImageB", "gate"))

            add(ConnectionRef("pair${index}ImageA", "value", "pair${index}ImageAImport", "path"))
            add(ConnectionRef("pair${index}ImageB", "value", "pair${index}ImageBImport", "path"))

            add(ConnectionRef("pair${index}ImageAImport", "image", "pair$index", "image_a"))
            add(ConnectionRef("pair${index}ImageBImport", "image", "pair$index", "image_b"))
            add(ConnectionRef("pair${index}LabelA", "result", "pair$index", "label_a"))
            add(ConnectionRef("pair${index}LabelB", "result", "pair$index", "label_b"))
            add(ConnectionRef("mascot${index % COMPARISON_MASCOT_POSES.size}Import", "image", "pair$index", "mascot"))
            add(ConnectionRef("pairDuration", "result", "pair$index", "duration_ms"))
            add(ConnectionRef("pair${index}ImageBImport", "image", "pair$index", "gate"))

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
