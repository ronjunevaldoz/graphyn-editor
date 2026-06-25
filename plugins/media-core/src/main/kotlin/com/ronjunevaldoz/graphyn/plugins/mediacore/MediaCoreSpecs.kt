package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

const val CATEGORY_MEDIA_VIDEO = "graphyn.media.video"
const val CATEGORY_MEDIA_AUDIO = "graphyn.media.audio"

object MediaCoreSpecs {
    val videoImport = NodeSpec(
        type = "media.video_import",
        label = "Video Import",
        description = "Loads a local video handle and reads its metadata with ffprobe.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("path", WorkflowType.StringType, description = "Local video path"),
        ),
        outputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
            PortSpec("duration_ms", WorkflowType.DoubleType),
            PortSpec("fps", WorkflowType.DoubleType),
        ),
        defaultValues = mapOf("path" to WorkflowValue.StringValue("")),
    )

    val audioExtract = NodeSpec(
        type = "media.audio_extract",
        label = "Audio Extract",
        description = "Extracts the first audio stream from a video as PCM WAV.",
        category = CATEGORY_MEDIA_AUDIO,
        inputs = listOf(PortSpec("video", MediaTypes.videoHandle)),
        outputs = listOf(
            PortSpec("audio", MediaTypes.audioHandle),
            PortSpec("sample_rate", WorkflowType.IntType),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
    )

    val audioMix = NodeSpec(
        type = "media.audio_mix",
        label = "Audio Mix",
        description = "Mixes audio tracks with optional per-track volume controls.",
        category = CATEGORY_MEDIA_AUDIO,
        inputs = listOf(
            PortSpec("audio_tracks", WorkflowType.ListType(MediaTypes.audioHandle)),
            PortSpec(
                "volumes",
                WorkflowType.ListType(WorkflowType.DoubleType),
                required = false,
                description = "One 0.0-1.0 volume per track; empty means 1.0",
            ),
        ),
        outputs = listOf(
            PortSpec("audio", MediaTypes.audioHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
        defaultValues = mapOf("volumes" to WorkflowValue.ListValue(emptyList())),
    )

    val videoStitch = NodeSpec(
        type = "media.video_stitch",
        label = "Video Stitch",
        description = "Concatenates compatible video clips using a cut transition.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("videos", WorkflowType.ListType(MediaTypes.videoHandle)),
            PortSpec("transition", WorkflowType.EnumType(listOf("cut"))),
        ),
        outputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
            PortSpec("frame_count", WorkflowType.IntType),
        ),
        defaultValues = mapOf("transition" to WorkflowValue.StringValue("cut")),
    )

    val videoEncode = NodeSpec(
        type = "media.video_encode",
        label = "Video Encode",
        description = "Encodes a video handle to H.264/AAC MP4.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec(
                "audio",
                WorkflowType.NullableType(MediaTypes.audioHandle),
                required = false,
            ),
            PortSpec("output_path", WorkflowType.StringType),
            PortSpec("bitrate", WorkflowType.EnumType(listOf("low", "medium", "high"))),
            PortSpec("codec", WorkflowType.EnumType(listOf("h264"))),
        ),
        outputs = listOf(
            PortSpec("file_path", WorkflowType.StringType),
            PortSpec("size_bytes", WorkflowType.DoubleType),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
        defaultValues = mapOf(
            "audio" to WorkflowValue.NullValue,
            "output_path" to WorkflowValue.StringValue("output.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
            "codec" to WorkflowValue.StringValue("h264"),
        ),
    )

    val all = listOf(videoImport, audioExtract, audioMix, videoStitch, videoEncode)
}
