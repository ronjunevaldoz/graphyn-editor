package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * End-to-end captioning workflow (media Phase 2).
 *
 * Import a video, extract its audio, transcribe it to timed caption segments, style the captions,
 * burn them into the video, and encode the captioned result.
 *
 * Demonstrates the Phase 2 captioning chain: `speech_to_text → caption_overlay`. Like the other
 * media-AI templates it is structurally complete but needs `GRAPHYN_STT_EXECUTABLE` (and an FFmpeg
 * built with libass) configured to actually run.
 */
internal val captionedVideoWorkflow = WorkflowDefinition(
    id = "captioned-video", name = "Captioned Video",
    nodes = listOf(
        guideNote(
            """
            Captioned Video

            Transcribes a video's speech and burns the captions back in.

            Flow: Resolve Path → Video Import → Audio Extract →
            Speech to Text → Caption Overlay (+ Caption Style) →
            Video Encode → Media Output.
            Use cases: subtitles, accessibility, social clips.
            Tips: Speech to Text emits timed segments the overlay consumes
            directly; Caption Style controls color, size, and position.
            """,
            height = 280,
        ),
        NodeRef("resolveVideo", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.mp4"),
        )),
        NodeRef("import_video", "media.video_import"),
        NodeRef("extract_audio", "media.audio_extract"),
        NodeRef("transcribe", "media.speech_to_text", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
        )),
        NodeRef("caption_style", CAPTION_STYLE_NODE_TYPE, config = CAPTION_STYLE_DEFAULTS),
        NodeRef("caption_overlay", "media.caption_overlay"),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("captioned.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolveVideo", "resolved_path", "import_video", "path"),
        ConnectionRef("import_video", "video", "extract_audio", "video"),
        ConnectionRef("extract_audio", "audio", "transcribe", "audio"),
        ConnectionRef("transcribe", "segments", "caption_overlay", "captions"),
        ConnectionRef("caption_style", "style_config", "caption_overlay", "style_config"),
        ConnectionRef("import_video", "video", "caption_overlay", "video"),
        ConnectionRef("caption_overlay", "video", "encode", "video"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)
