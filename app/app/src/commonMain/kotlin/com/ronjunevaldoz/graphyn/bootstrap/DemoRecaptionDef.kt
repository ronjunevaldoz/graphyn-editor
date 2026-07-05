package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)

/**
 * Restyles captions/narration on an already-stitched clip without redoing the Ollama + Flux
 * generation that produced it — reads back the `.stitched.mp4` and `.storyboard.json` that
 * [imageMotionStoryboardShortWorkflow] persists (see [STORYBOARD_OUTPUT_BASE]).
 */
internal fun recaptionWorkflow(
    stitchedVideoPath: String,
    storyboardJsonPath: String,
    styleOverrides: Map<String, WorkflowValue> = CAPTION_STYLE_DEFAULTS,
    outputPath: String = "$STORYBOARD_OUTPUT_BASE.recaptioned.mp4",
) = WorkflowDefinition(
    id = "image-motion-storyboard-recaption",
    name = "Image Motion Short (Recaption)",
    nodes = listOf(
        NodeRef("videoIn", "media.video_import", config = mapOf("path" to s(stitchedVideoPath))),
        NodeRef("storyboardRead", "io.file_read", config = mapOf("path" to s(storyboardJsonPath))),
        NodeRef("storyboardParse", "json.parse"),
        NodeRef("narration", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("narration"))),
        NodeRef("captionsScript", STORYBOARD_CAPTIONS_NODE_TYPE, config = mapOf(
            "scene_duration_ms" to d(STORYBOARD_SCENE_DURATION_MS),
        )),
        NodeRef("captionStyle", CAPTION_STYLE_NODE_TYPE, config = CAPTION_STYLE_DEFAULTS + styleOverrides),
        NodeRef("captionOverlay", "media.caption_overlay"),
        NodeRef("narrate", "media.text_to_speech", config = mapOf(
            "language" to s("en"), "voice_id" to s("Samantha"), "speed" to d(1.0),
        )),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to s(outputPath), "bitrate" to s("high"), "codec" to s("h264"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("storyboardRead", "content", "storyboardParse", "text"),
        ConnectionRef("storyboardParse", "value", "narration", "input"),
        ConnectionRef("storyboardParse", "value", "captionsScript", "input"),
        ConnectionRef("videoIn", "video", "captionOverlay", "video"),
        ConnectionRef("captionsScript", "result", "captionOverlay", "captions"),
        ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"),
        ConnectionRef("narration", "result", "narrate", "text"),
        ConnectionRef("captionOverlay", "video", "encode", "video"),
        ConnectionRef("narrate", "audio", "encode", "audio"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)
