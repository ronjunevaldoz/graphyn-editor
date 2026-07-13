package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

/**
 * Regenerates a single scene, re-stitches it with the *other* scenes' already-saved clips (see
 * [STORYBOARD_OUTPUT_BASE] in [imageMotionStoryboardShortWorkflow]) instead of redoing every
 * scene, then reapplies captions/narration from the saved storyboard JSON to produce a fresh
 * final clip. Two modes for the regenerated scene itself:
 * - full regen (default): a fresh FLUX generation from `prompt`/`niche`/`visualStyle`/`character`,
 *   ignoring whatever image was there before.
 * - [editMode]: a targeted FLUX Kontext edit of the scene's *existing* image
 *   ([editReferenceImagePath], from `imageMotionSceneSubgraphDynamic`'s `imagePathSidecarPath`),
 *   driven by [editInstruction] (a change, not a scene description) — see
 *   [imageMotionSceneEditSubgraph]. Deciding whether a sidecar exists and requiring
 *   [editInstruction] to be non-null are the caller's (`WorkflowCliRunner.kt`) job, not this
 *   function's — this is a pure builder, no file I/O.
 */
internal fun regenerateSceneWorkflow(
    sceneIndex: Int,
    prompt: String,
    niche: String,
    visualStyle: String,
    character: String,
    storyboardJsonPath: String,
    outputPath: String? = null,
    ttsEngine: TtsEngineChoice = TtsEngineChoice("say", mapOf("voice_id" to s("Samantha"), "speed" to d(1.0))),
    editMode: Boolean = false,
    editReferenceImagePath: String? = null,
    editInstruction: String? = null,
): WorkflowDefinition {
    val outputPath = outputPath ?: "$STORYBOARD_OUTPUT_BASE.mp4"
    return WorkflowDefinition(
    id = "image-motion-storyboard-regenerate-scene",
    name = "Image Motion Short (Regenerate Scene $sceneIndex)",
    nodes = buildList {
        add(NodeRef("unloadOllama", OLLAMA_UNLOAD_NODE_TYPE))
        // Both branches persist their result's image path to the same sidecar the storyboard
        // run writes — otherwise a later edit would condition on whatever image existed *before*
        // this regeneration/edit instead of the one it just produced.
        val sidecarPath = "$STORYBOARD_OUTPUT_BASE.scene$sceneIndex.image.txt"
        val regenSceneSubgraph = if (editMode) {
            imageMotionSceneEditSubgraph(
                id = "regen-scene-edit-$sceneIndex",
                referenceImagePath = requireNotNull(editReferenceImagePath) { "editMode requires editReferenceImagePath" },
                editInstruction = requireNotNull(editInstruction) { "editMode requires editInstruction" },
                imageCount = STORYBOARD_IMAGES_PER_SCENE,
                imagePathSidecarPath = sidecarPath,
            )
        } else {
            imageMotionSceneSubgraph(
                prompt = prompt, niche = niche, imageCount = STORYBOARD_IMAGES_PER_SCENE,
                visualStyle = visualStyle, character = character,
                imagePathSidecarPath = sidecarPath,
            )
        }
        add(NodeRef("regenScene", SHORTS_SCENE_SUBGRAPH_NODE_TYPE, subgraph = regenSceneSubgraph))
        add(NodeRef("regenSceneSave", "media.video_encode", config = mapOf(
            "output_path" to s("$STORYBOARD_OUTPUT_BASE.scene$sceneIndex.mp4"), "bitrate" to s("medium"), "codec" to s("h264"),
        )))
        repeat(STORYBOARD_SCENE_COUNT) { index ->
            if (index != sceneIndex) {
                add(NodeRef("scene${index}Import", "media.video_import", config = mapOf(
                    "path" to s("$STORYBOARD_OUTPUT_BASE.scene$index.mp4"),
                )))
            }
        }
        add(NodeRef("stitch", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(0)))
        add(NodeRef("stitchSave", "media.video_encode", config = mapOf(
            "output_path" to s("$STORYBOARD_OUTPUT_BASE.stitched.mp4"), "bitrate" to s("medium"), "codec" to s("h264"),
        )))
        add(NodeRef("storyboardRead", "io.file_read", config = mapOf("path" to s(storyboardJsonPath))))
        add(NodeRef("storyboardParse", "json.parse"))
        add(NodeRef("narration", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("narration"))))
        add(NodeRef("captionsScript", STORYBOARD_CAPTIONS_NODE_TYPE, config = mapOf("scene_duration_ms" to d(STORYBOARD_SCENE_DURATION_MS))))
        add(shortsCaptionStyleNode())
        add(NodeRef("captionOverlay", "media.caption_overlay"))
        add(NodeRef("narrate", "media.text_to_speech.${ttsEngine.engine}", config = ttsEngine.params))
        add(NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to s(outputPath), "bitrate" to s("high"), "codec" to s("h264"),
        )))
        add(NodeRef("output", "media.file_output"))
    },
    connections = buildList {
        add(ConnectionRef("unloadOllama", "gate", "regenScene", "gate"))
        add(ConnectionRef("regenScene", "video", "regenSceneSave", "video"))
        add(ConnectionRef("regenScene", "video", "stitch", "video${sceneIndex + 1}"))
        repeat(STORYBOARD_SCENE_COUNT) { index ->
            if (index != sceneIndex) {
                add(ConnectionRef("scene${index}Import", "video", "stitch", "video${index + 1}"))
            }
        }
        add(ConnectionRef("stitch", "video", "stitchSave", "video"))
        add(ConnectionRef("stitch", "video", "captionOverlay", "video"))
        add(ConnectionRef("storyboardRead", "content", "storyboardParse", "text"))
        add(ConnectionRef("storyboardParse", "value", "narration", "input"))
        add(ConnectionRef("storyboardParse", "value", "captionsScript", "input"))
        add(ConnectionRef("captionsScript", "result", "captionOverlay", "captions"))
        add(ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"))
        add(ConnectionRef("narration", "result", "narrate", "text"))
        add(ConnectionRef("captionOverlay", "video", "encode", "video"))
        add(ConnectionRef("narrate", "audio", "encode", "audio"))
        add(ConnectionRef("encode", "file_path", "output", "file_path"))
    },
    )
}
