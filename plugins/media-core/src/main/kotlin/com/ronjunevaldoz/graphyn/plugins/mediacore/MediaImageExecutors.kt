package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Executor factories for the Phase 3 image-processing nodes, registered by [MediaCorePlugin]. */
internal fun imageResizeExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.resizeImage(
        imagePath = MediaTypes.path(inputs["image"], "image"),
        width = inputs.intField("width"),
        height = inputs.intField("height"),
    )
    metadata.toOutputs()
}

internal fun imageCropExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val metadata = backend.cropImage(
        imagePath = MediaTypes.path(inputs["image"], "image"),
        x = inputs.intField("x"),
        y = inputs.intField("y"),
        width = inputs.intField("width"),
        height = inputs.intField("height"),
    )
    metadata.toOutputs()
}

internal fun imageSequenceToVideoExecutor(backend: MediaCoreBackend) = NodeExecutor { inputs ->
    val paths = inputs.list("images").map { MediaTypes.path(it, "image") }
    val metadata = backend.imageSequenceToVideo(paths, inputs.numberOr("fps", 1.0))
    mapOf(
        "video" to MediaTypes.videoValue(metadata.path),
        "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
        "frame_count" to WorkflowValue.IntValue(metadata.frameCount),
    )
}

private fun ImageMetadata.toOutputs(): Map<String, WorkflowValue> = mapOf(
    "image" to MediaTypes.imageValue(path),
    "width" to WorkflowValue.IntValue(width),
    "height" to WorkflowValue.IntValue(height),
)
