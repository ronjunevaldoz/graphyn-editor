package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Phase 2 captioning, composition, image, and audio-output specs. Kept in a dedicated object so
 * [MediaCoreSpecs] stays focused on the Phase 1 decode/encode primitives and every spec file stays
 * under the size ceiling.
 */
object MediaCompositionSpecs {
    val audioEncode = NodeSpec(
        type = "media.audio_encode",
        label = "Audio Encode",
        description = "Encodes an audio handle to a WAV/MP3/AAC file so audio workflows can save output.",
        category = CATEGORY_MEDIA_AUDIO,
        inputs = listOf(
            PortSpec("audio", MediaTypes.audioHandle),
            PortSpec("output_path", WorkflowType.StringType),
            PortSpec("format", WorkflowType.EnumType(listOf("wav", "mp3", "aac"))),
        ),
        outputs = listOf(
            PortSpec("file_path", WorkflowType.StringType),
            PortSpec("size_bytes", WorkflowType.DoubleType),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
        defaultValues = mapOf(
            "output_path" to WorkflowValue.StringValue("output.wav"),
            "format" to WorkflowValue.StringValue("wav"),
        ),
    )

    val imageImport = NodeSpec(
        type = "media.image_import",
        label = "Image Import",
        description = "Loads a local image handle and reads its pixel dimensions with ffprobe.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(PortSpec("path", WorkflowType.StringType, description = "Local image path")),
        outputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
    )

    val captionStyleType = WorkflowType.RecordType(
        mapOf(
            "color" to WorkflowType.StringType,
            "background_color" to WorkflowType.StringType,
            "font_size" to WorkflowType.IntType,
            "position" to WorkflowType.EnumType(listOf("top", "center", "bottom")),
        ),
    )

    val captionOverlay = NodeSpec(
        type = "media.caption_overlay",
        label = "Caption Overlay",
        description = "Burns timed captions onto a video using a caption style.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("captions", MediaCompositionTypes.captions),
            PortSpec("style_config", captionStyleType),
        ),
        outputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
    )

    val videoCompose = NodeSpec(
        type = "media.video_compose",
        label = "Video Compose",
        description = "Layers overlay clips over a base video with per-overlay timing and opacity.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("base_video", MediaTypes.videoHandle),
            PortSpec("overlays", MediaCompositionTypes.videoOverlays),
        ),
        outputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
    )

    val timingController = NodeSpec(
        type = "media.timing_controller",
        label = "Timing Controller",
        description = "Averages measured sync points into video/audio/caption delays for downstream nodes.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("base_video", MediaTypes.videoHandle),
            PortSpec("audio_track", MediaTypes.audioHandle, required = false),
            PortSpec("sync_points", MediaCompositionTypes.syncPoints, required = false),
        ),
        outputs = listOf(PortSpec("config", MediaCompositionTypes.timingConfig)),
    )

    val all = listOf(audioEncode, imageImport, captionOverlay, videoCompose, timingController)
}
