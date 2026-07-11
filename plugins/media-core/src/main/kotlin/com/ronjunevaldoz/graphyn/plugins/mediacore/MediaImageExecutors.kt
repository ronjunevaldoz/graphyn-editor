package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.intOrError
import com.ronjunevaldoz.graphyn.core.model.listOrError
import com.ronjunevaldoz.graphyn.core.model.numberOr
import com.ronjunevaldoz.graphyn.core.model.stringOr

/** Executor factories for the Phase 3 image-processing nodes, registered by [MediaCorePlugin]. */
internal fun imageResizeExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.resizeImage(
        imagePath = MediaTypes.path(inputs["image"], "image"),
        width = inputs.intOrError("width"),
        height = inputs.intOrError("height"),
    )
    metadata.toOutputs()
}

internal fun imageCropExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.cropImage(
        imagePath = MediaTypes.path(inputs["image"], "image"),
        x = inputs.intOrError("x"),
        y = inputs.intOrError("y"),
        width = inputs.intOrError("width"),
        height = inputs.intOrError("height"),
    )
    metadata.toOutputs()
}

internal fun imageFlipExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.flipImage(imagePath = MediaTypes.path(inputs["image"], "image"))
    metadata.toOutputs()
}

internal fun imageSequenceToVideoExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val paths = inputs.listOrError("images").map { MediaTypes.path(it, "image") }
    val metadata = backend.imageSequenceToVideo(paths, inputs.numberOr("fps", 1.0))
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
        "frame_count" to WorkflowValue.IntValue(metadata.frameCount),
    )
}

internal fun kenBurnsExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.kenBurns(
        imagePath = MediaTypes.path(inputs["image"], "image"),
        durationMs = inputs.numberOr("duration_ms", 2000.0),
        fps = inputs.numberOr("fps", 24.0),
        zoomStart = inputs.numberOr("zoom_start", 1.0),
        zoomEnd = inputs.numberOr("zoom_end", 1.15),
        panX = inputs.stringOr("pan_x", "center"),
        panY = inputs.stringOr("pan_y", "center"),
        width = inputs.numberOr("width", 720.0).toInt(),
        height = inputs.numberOr("height", 1280.0).toInt(),
    )
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
    )
}

private fun ImageMetadata.toOutputs(): Map<String, WorkflowValue> = mapOf(
    "image" to MediaTypes.imageValue(path),
    "width" to WorkflowValue.IntValue(width),
    "height" to WorkflowValue.IntValue(height),
)
