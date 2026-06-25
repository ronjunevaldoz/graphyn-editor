package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Executor factories for the Phase 2 composition nodes, registered by [MediaCorePlugin]. */
internal fun captionOverlayExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val captions = inputs.list("captions").map { it.toCaption() }
    val metadata = backend.overlayCaptions(
        videoPath = MediaTypes.path(inputs["video"], "video"),
        captions = captions,
        style = inputs.record("style_config").toCaptionStyle(),
    )
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
    )
}

internal fun videoComposeExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val overlays = inputs.list("overlays").map { it.toVideoOverlay() }
    val metadata = backend.composeVideo(
        baseVideoPath = MediaTypes.path(inputs["base_video"], "video"),
        overlays = overlays,
    )
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
    )
}

internal fun timingControllerExecutor() = NodeExecutor { inputs ->
    MediaTypes.path(inputs["base_video"], "video")
    val syncPoints = inputs.listOrEmpty("sync_points").map { it.record() }
    val audioDelayMs = if (syncPoints.isEmpty()) {
        0.0
    } else {
        syncPoints.sumOf { it.getValue("target_ms").number() - it.getValue("source_ms").number() } / syncPoints.size
    }
    mapOf(
        "config" to MediaCompositionTypes.timingConfigValue(
            videoDelayMs = 0.0,
            audioDelayMs = audioDelayMs,
            captionOffsetMs = audioDelayMs,
        ),
    )
}

private fun WorkflowValue.record(): Map<String, WorkflowValue> =
    (this as? WorkflowValue.RecordValue)?.fields ?: error("Expected a record value.")

private fun WorkflowValue.toCaption(): Caption {
    val fields = record()
    return Caption(
        text = (fields["text"] as? WorkflowValue.StringValue)?.value ?: error("Caption is missing text."),
        startMs = fields.getValue("start_ms").number(),
        endMs = fields.getValue("end_ms").number(),
    )
}

private fun Map<String, WorkflowValue>.toCaptionStyle(): CaptionStyle = CaptionStyle(
    color = (getValue("color") as WorkflowValue.StringValue).value,
    backgroundColor = (getValue("background_color") as WorkflowValue.StringValue).value,
    fontSize = intField("font_size"),
    position = (getValue("position") as WorkflowValue.StringValue).value,
)

private fun WorkflowValue.toVideoOverlay(): VideoOverlay {
    val fields = record()
    return VideoOverlay(
        sourcePath = MediaTypes.path(fields["source"], "video"),
        x = (fields["x"] as? WorkflowValue.IntValue)?.value ?: 0,
        y = (fields["y"] as? WorkflowValue.IntValue)?.value ?: 0,
        startMs = fields.getValue("start_ms").number(),
        endMs = fields.getValue("end_ms").number(),
        opacity = fields["opacity"]?.number() ?: 1.0,
    )
}
