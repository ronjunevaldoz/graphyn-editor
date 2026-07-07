package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)

internal const val STORYBOARD_IMAGES_PER_SCENE = 2
internal const val STORYBOARD_SCENE_DURATION_MS = STORYBOARD_IMAGES_PER_SCENE * 1000.0
internal const val STORYBOARD_OUTPUT_BASE = "image-motion-storyboard-short"

/**
 * Storyboard-first image-motion short: an Ollama-generated, validated storyboard (niche, shared
 * visual style, narration, per-scene prompt+caption) drives [STORYBOARD_SCENE_COUNT] Flux scenes,
 * captions, and narration — instead of hand-authoring them, like [imageMotionShortWorkflow] does.
 * All fields are extracted defensively (see [storyboardGeneratorSubgraph]), so a malformed LLM
 * response falls back to a fixed storyboard rather than failing deep in the pipeline.
 */
internal fun imageMotionStoryboardShortWorkflow(
    topic: String,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    // Off by default: a character-sheet reference image + FLUX Kontext conditioning gives real
    // cross-scene visual consistency, but costs materially more time per scene (20 sampling steps
    // instead of 4, plus reference-image conditioning) — keep the cheap bare-Ken-Burns path as the
    // default for iterating on storyboard/caption/TTS/stitch correctness, opt into this only when
    // actually validating character consistency.
    useCharacterSheet: Boolean = false,
) = WorkflowDefinition(
    id = "image-motion-storyboard-short",
    name = "Image Motion Short (Storyboard)",
    nodes = buildList {
        add(guideNote(
            """
            Image Motion Short · storyboard-first

            An Ollama-generated storyboard (niche, visual style, narration,
            per-scene prompt+caption) drives every scene, so the narration
            and on-screen captions come from the same authored script instead
            of being reconciled after the fact.
            """,
        ))
        add(NodeRef("storyboard", STORYBOARD_SUBGRAPH_NODE_TYPE, subgraph = storyboardGeneratorSubgraph(topic)))
        add(NodeRef("niche", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("niche"))))
        add(NodeRef("visualStyle", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("visual_style"))))
        add(NodeRef("character", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("character"))))
        add(NodeRef("narration", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("narration"))))
        add(NodeRef("captionsScript", STORYBOARD_CAPTIONS_NODE_TYPE, config = mapOf("scene_duration_ms" to d(STORYBOARD_SCENE_DURATION_MS))))
        if (useCharacterSheet) {
            // Three separate reference images (neutral/smiling/action), not one grid — Kontext
            // conditions on a reference image as a whole and can't be told "just use this panel"
            // (see CharacterSheetPoses' KDoc) — each is its own outer node so all three can be
            // wired into the same scene's list-typed ref_images port.
            add(NodeRef(
                "characterSheetNeutral", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = characterSheetSubgraphDynamic(
                    id = "storyboard-character-sheet-neutral", width = width, height = height,
                    poseInstruction = CharacterSheetPoses.NEUTRAL,
                ),
            ))
            add(NodeRef(
                "characterSheetSmiling", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = characterSheetSubgraphDynamic(
                    id = "storyboard-character-sheet-smiling", width = width, height = height,
                    poseInstruction = CharacterSheetPoses.SMILING,
                ),
            ))
            add(NodeRef(
                "characterSheetAction", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = characterSheetSubgraphDynamic(
                    id = "storyboard-character-sheet-action", width = width, height = height,
                    poseInstruction = CharacterSheetPoses.ACTION,
                ),
            ))
        }
        repeat(STORYBOARD_SCENE_COUNT) { index ->
            add(NodeRef("scene${index}Prompt", STORYBOARD_SCENE_FIELD_NODE_TYPE, config = mapOf("index" to i(index), "field" to s("prompt"))))
            add(NodeRef(
                "scene$index", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = imageMotionSceneSubgraphDynamic(
                    id = "storyboard-scene-$index", imageCount = STORYBOARD_IMAGES_PER_SCENE, width = width, height = height,
                    useCharacterSheet = useCharacterSheet,
                    // Unconditional, same as the .sceneN.mp4 clip below — lets a later
                    // regenerate-scene edit=true run condition on this exact keyframe.
                    imagePathSidecarPath = "$STORYBOARD_OUTPUT_BASE.scene$index.image.txt",
                ),
            ))
            // Persists this scene's raw clip so a later run can regenerate just one scene and
            // re-stitch using the other, still-valid saved clips instead of redoing everything.
            add(NodeRef("scene${index}Save", "media.video_encode", config = mapOf(
                "output_path" to s("$STORYBOARD_OUTPUT_BASE.scene$index.mp4"), "bitrate" to s("medium"), "codec" to s("h264"),
            )))
        }
        add(NodeRef("stitch", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(0)))
        // Persists the pre-caption stitched clip so a "recaption" run can restyle captions/narration
        // without redoing the Ollama + Flux generation that produced it.
        add(NodeRef("stitchSave", "media.video_encode", config = mapOf(
            "output_path" to s("$STORYBOARD_OUTPUT_BASE.stitched.mp4"), "bitrate" to s("medium"), "codec" to s("h264"),
        )))
        add(shortsCaptionStyleNode())
        add(NodeRef("captionOverlay", "media.caption_overlay"))
        // "", not a named speaker — see DemoRecaptionDef.kt's ttsEngine default for why.
        add(NodeRef("narrate", "media.text_to_speech.qwen3", config = mapOf("voice" to s(""))))
        add(NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to s("$STORYBOARD_OUTPUT_BASE.mp4"), "bitrate" to s("high"), "codec" to s("h264"),
        )))
        add(NodeRef("output", "media.file_output"))
        // Persists niche/visual_style/character/narration/scenes so recaption/regenerate-scene runs
        // can reuse them without calling Ollama again.
        add(NodeRef("storyboardJson", "json.stringify", config = mapOf("pretty" to WorkflowValue.BooleanValue(true))))
        add(NodeRef("storyboardJsonWrite", "io.file_write", config = mapOf(
            "path" to s("$STORYBOARD_OUTPUT_BASE.storyboard.json"), "append" to WorkflowValue.BooleanValue(false),
        )))
    },
    connections = buildList {
        add(ConnectionRef("storyboard", "value", "niche", "input"))
        add(ConnectionRef("storyboard", "value", "visualStyle", "input"))
        add(ConnectionRef("storyboard", "value", "character", "input"))
        add(ConnectionRef("storyboard", "value", "narration", "input"))
        add(ConnectionRef("storyboard", "value", "captionsScript", "input"))
        if (useCharacterSheet) {
            add(ConnectionRef("character", "result", "characterSheetNeutral", "character"))
            add(ConnectionRef("character", "result", "characterSheetSmiling", "character"))
            add(ConnectionRef("character", "result", "characterSheetAction", "character"))
            // Serialize the three character-sheet generations too, same reasoning as scene-to-scene
            // gating below — one shared 12GB-class GPU, avoid firing concurrent requests at it.
            add(ConnectionRef("characterSheetNeutral", "image", "characterSheetSmiling", "gate"))
            add(ConnectionRef("characterSheetSmiling", "image", "characterSheetAction", "gate"))
        }
        repeat(STORYBOARD_SCENE_COUNT) { index ->
            add(ConnectionRef("storyboard", "value", "scene${index}Prompt", "input"))
            add(ConnectionRef("scene${index}Prompt", "result", "scene$index", "prompt"))
            add(ConnectionRef("niche", "result", "scene$index", "niche"))
            add(ConnectionRef("visualStyle", "result", "scene$index", "visual_style"))
            add(ConnectionRef("character", "result", "scene$index", "character"))
            if (useCharacterSheet) {
                // ref_images: all three character-sheet variants collect into one list at this
                // boundary (buildInputMap in WorkflowExecutionScheduling.kt collects every incoming
                // connection whenever the target port is list-typed) — no wrapper node needed.
                add(ConnectionRef("characterSheetNeutral", "image", "scene$index", "ref_images"))
                add(ConnectionRef("characterSheetSmiling", "image", "scene$index", "ref_images"))
                add(ConnectionRef("characterSheetAction", "image", "scene$index", "ref_images"))
            }
            if (index > 0) {
                // Serializes scene generation — without this, all scenes hit server-sd concurrently
                // and fight over the shared 12GB GPU (same "gate" pattern as DemoShortsVideoWorkflow).
                add(ConnectionRef("scene${index - 1}", "video", "scene$index", "gate"))
            } else if (useCharacterSheet) {
                // Scene 0 has no prior scene to gate on — gate it on the last character-sheet
                // variant instead, so it can't start before all three reference images exist.
                // Scenes 1+ are already transitively ordered after this via the scene-to-scene
                // gate chain.
                add(ConnectionRef("characterSheetAction", "image", "scene0", "gate"))
            }
            add(ConnectionRef("scene$index", "video", "stitch", "video${index + 1}"))
            add(ConnectionRef("scene$index", "video", "scene${index}Save", "video"))
        }
        add(ConnectionRef("stitch", "video", "captionOverlay", "video"))
        add(ConnectionRef("stitch", "video", "stitchSave", "video"))
        add(ConnectionRef("captionsScript", "result", "captionOverlay", "captions"))
        add(ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"))
        add(ConnectionRef("narration", "result", "narrate", "text"))
        add(ConnectionRef("captionOverlay", "video", "encode", "video"))
        add(ConnectionRef("narrate", "audio", "encode", "audio"))
        add(ConnectionRef("encode", "file_path", "output", "file_path"))
        add(ConnectionRef("storyboard", "value", "storyboardJson", "value"))
        add(ConnectionRef("storyboardJson", "text", "storyboardJsonWrite", "content"))
    },
)
