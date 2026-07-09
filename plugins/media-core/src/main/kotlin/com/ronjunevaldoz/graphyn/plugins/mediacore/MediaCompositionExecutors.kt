package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.listOrEmpty
import com.ronjunevaldoz.graphyn.core.model.listOrError
import com.ronjunevaldoz.graphyn.core.model.intOrError
import com.ronjunevaldoz.graphyn.core.model.numberOrError
import com.ronjunevaldoz.graphyn.core.model.record
import com.ronjunevaldoz.graphyn.core.model.recordOrError
import com.ronjunevaldoz.graphyn.core.model.stringOr
import com.ronjunevaldoz.graphyn.core.model.stringOrError
import com.ronjunevaldoz.graphyn.plugins.mediacore.mapper.toCaptionStyle
import com.ronjunevaldoz.graphyn.plugins.mediacore.mapper.toComparisonLayoutStyle
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.Caption

/** Executor factories for the Phase 2 image/composition nodes, registered by [MediaCorePlugin]. */
internal fun imageImportExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.inspectImage(inputs.stringOrError("path"))
    mapOf(
        "image" to MediaTypes.imageValue(metadata.path),
        "width" to WorkflowValue.IntValue(metadata.width),
        "height" to WorkflowValue.IntValue(metadata.height),
    )
}

internal fun captionOverlayExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val captions = inputs.listOrError("captions").map { it.toCaption() }
    val metadata = backend.overlayCaptions(
        videoPath = MediaTypes.path(inputs["video"], "video"),
        captions = captions,
        style = inputs.recordOrError("style_config").toCaptionStyle(),
    )
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
    )
}

internal fun videoComposeExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val overlays = inputs.listOrError("overlays").map { it.toVideoOverlay() }
    val metadata = backend.composeVideo(
        baseVideoPath = MediaTypes.path(inputs["base_video"], "video"),
        overlays = overlays,
    )
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
    )
}

internal fun comparisonLayoutExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.compositeComparisonLayout(
        imageAPath = MediaTypes.path(inputs["image_a"], "image"),
        imageBPath = MediaTypes.path(inputs["image_b"], "image"),
        labelA = inputs.stringOrError("label_a"),
        labelB = inputs.stringOrError("label_b"),
        caption = inputs.stringOr("caption", ""),
        mascotPath = MediaTypes.path(inputs["mascot"], "image"),
        style = inputs.recordOrError("style_config").toComparisonLayoutStyle(),
        width = inputs.intOrError("width"),
        height = inputs.intOrError("height"),
    )
    mapOf(
        "image" to MediaTypes.imageValue(metadata.path),
    )
}

internal fun audioEncodeExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val encoded = backend.encodeAudio(
        audioPath = MediaTypes.path(inputs["audio"], "audio"),
        outputPath = inputs.stringOrError("output_path"),
        format = inputs.stringOr("format", "wav"),
    )
    mapOf(
        "file_path" to WorkflowValue.StringValue(encoded.path),
        "size_bytes" to WorkflowValue.DoubleValue(encoded.sizeBytes.toDouble()),
        "duration_ms" to WorkflowValue.DoubleValue(encoded.durationMs),
    )
}

internal fun timingControllerExecutor() = NodeExecutor { inputs ->
    MediaTypes.path(inputs["base_video"], "video")
    val syncPoints = inputs.listOrEmpty("sync_points").map { it.record() }
    val audioDelayMs = if (syncPoints.isEmpty()) {
        0.0
    } else {
        syncPoints.sumOf {
            it.getValue("target_ms").numberOrError() - it.getValue("source_ms").numberOrError()
        } / syncPoints.size
    }
    mapOf(
        "config" to MediaCompositionTypes.timingConfigValue(
            videoDelayMs = 0.0,
            audioDelayMs = audioDelayMs,
            captionOffsetMs = audioDelayMs,
        ),
    )
}

fun WorkflowValue.toCaption(): Caption {
    val fields = record()
    return Caption(
        text = (fields["text"] as? WorkflowValue.StringValue)?.value
            ?: error("Caption is missing text."),
        startMs = fields.getValue("start_ms").numberOrError(),
        endMs = fields.getValue("end_ms").numberOrError(),
    )
}

fun WorkflowValue.toVideoOverlay(): VideoOverlay {
    val fields = record()
    return VideoOverlay(
        sourcePath = MediaTypes.path(fields["source"], "video"),
        x = (fields["x"] as? WorkflowValue.IntValue)?.value ?: 0,
        y = (fields["y"] as? WorkflowValue.IntValue)?.value ?: 0,
        startMs = fields.getValue("start_ms").numberOrError(),
        endMs = fields.getValue("end_ms").numberOrError(),
        opacity = fields["opacity"]?.numberOrError() ?: 1.0,
    )
}
