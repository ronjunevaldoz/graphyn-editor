package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionAlignment

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

    // TODO convert to CaptionStyleNode with color picker and allow hexcolor
    val captionStyleType = WorkflowType.RecordType(
        mapOf(
            "font_family" to WorkflowType.StringType,
            "font_size" to WorkflowType.IntType,
            "text_color" to WorkflowType.StringType,
            "background_color" to WorkflowType.NullableType(WorkflowType.StringType),
            "outline_color" to WorkflowType.StringType,
            "outline_width" to WorkflowType.IntType,
            "shadow" to WorkflowType.IntType,
            "bold" to WorkflowType.BooleanType,
            "italic" to WorkflowType.BooleanType,
            "alignment" to WorkflowType.EnumType(
                CaptionAlignment.entries.map { it.name },
            ),
            "margin_horizontal" to WorkflowType.IntType,
            "margin_vertical" to WorkflowType.IntType,
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

    val comparisonLayoutStyleType = WorkflowType.RecordType(
        mapOf(
            "background_color" to WorkflowType.StringType,
            "label_font_family" to WorkflowType.StringType,
            "label_font_size" to WorkflowType.IntType,
            "label_color" to WorkflowType.StringType,
            "caption_font_family" to WorkflowType.StringType,
            "caption_font_size" to WorkflowType.IntType,
            "caption_color" to WorkflowType.StringType,
            "panel_gap" to WorkflowType.IntType,
        ),
    )

    val comparisonLayout = NodeSpec(
        type = "media.comparison_layout",
        label = "Comparison Layout",
        description = "Composites two labeled images, a caption, and a mascot image into one " +
            "still frame — the \"X vs Y\" comparison-explainer layout.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("image_a", MediaTypes.imageHandle),
            PortSpec("image_b", MediaTypes.imageHandle),
            PortSpec("label_a", WorkflowType.StringType),
            PortSpec("label_b", WorkflowType.StringType),
            PortSpec(
                "caption", WorkflowType.StringType, required = false,
                description = "Optional text baked into the still frame itself. The comparison " +
                    "short's question/answer beats are timed subtitle-style overlays applied " +
                    "later by media.caption_overlay on the stitched video (same convention every " +
                    "other short in this pipeline uses) — leave this blank unless extra baked-in " +
                    "text is wanted on top of that.",
            ),
            PortSpec("mascot", MediaTypes.imageHandle),
            PortSpec("style_config", comparisonLayoutStyleType),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
        outputs = listOf(PortSpec("image", MediaTypes.imageHandle)),
        defaultValues = mapOf(
            "caption" to WorkflowValue.StringValue(""),
            "width" to WorkflowValue.IntValue(720),
            "height" to WorkflowValue.IntValue(1280),
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

    val all = listOf(audioEncode, imageImport, captionOverlay, videoCompose, comparisonLayout, timingController)
}
