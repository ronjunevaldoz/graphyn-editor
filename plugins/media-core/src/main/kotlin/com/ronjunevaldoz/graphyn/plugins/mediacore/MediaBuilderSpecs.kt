package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * In-graph record builders for the composition nodes. [MediaCompositionSpecs.videoCompose] and
 * [MediaCompositionSpecs.timingController] consume *lists of records* with no other producer, so
 * these builder + collector nodes let a user assemble those inputs on the canvas.
 */
object MediaBuilderSpecs {
    val videoOverlay = NodeSpec(
        type = "media.video_overlay",
        label = "Video Overlay",
        description = "Builds one overlay layer (source, position, time window, opacity) for Video Compose.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("source", MediaTypes.videoHandle),
            PortSpec("x", WorkflowType.IntType),
            PortSpec("y", WorkflowType.IntType),
            PortSpec("start_ms", WorkflowType.DoubleType),
            PortSpec("end_ms", WorkflowType.DoubleType),
            PortSpec("opacity", WorkflowType.DoubleType),
        ),
        outputs = listOf(PortSpec("overlay", MediaCompositionTypes.videoOverlay)),
        defaultValues = mapOf(
            "x" to WorkflowValue.IntValue(0),
            "y" to WorkflowValue.IntValue(0),
            "start_ms" to WorkflowValue.DoubleValue(0.0),
            "end_ms" to WorkflowValue.DoubleValue(0.0),
            "opacity" to WorkflowValue.DoubleValue(1.0),
        ),
    )

    val overlaysList = NodeSpec(
        type = "media.overlays_list",
        label = "Overlays List",
        description = "Collects overlay layers into the list Video Compose expects.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("overlay1", MediaCompositionTypes.videoOverlay),
            PortSpec("overlay2", MediaCompositionTypes.videoOverlay, required = false),
            PortSpec("overlay3", MediaCompositionTypes.videoOverlay, required = false),
            PortSpec("overlay4", MediaCompositionTypes.videoOverlay, required = false),
        ),
        outputs = listOf(PortSpec("overlays", MediaCompositionTypes.videoOverlays)),
    )

    val syncPoint = NodeSpec(
        type = "media.sync_point",
        label = "Sync Point",
        description = "Builds one (source_ms, target_ms) measurement for the Timing Controller.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("source_ms", WorkflowType.DoubleType),
            PortSpec("target_ms", WorkflowType.DoubleType),
        ),
        outputs = listOf(PortSpec("point", MediaCompositionTypes.syncPoint)),
        defaultValues = mapOf(
            "source_ms" to WorkflowValue.DoubleValue(0.0),
            "target_ms" to WorkflowValue.DoubleValue(0.0),
        ),
    )

    val syncPointsList = NodeSpec(
        type = "media.sync_points_list",
        label = "Sync Points List",
        description = "Collects sync points into the list the Timing Controller averages.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("point1", MediaCompositionTypes.syncPoint),
            PortSpec("point2", MediaCompositionTypes.syncPoint, required = false),
            PortSpec("point3", MediaCompositionTypes.syncPoint, required = false),
            PortSpec("point4", MediaCompositionTypes.syncPoint, required = false),
        ),
        outputs = listOf(PortSpec("sync_points", MediaCompositionTypes.syncPoints)),
    )

    val all = listOf(videoOverlay, overlaysList, syncPoint, syncPointsList)
}
