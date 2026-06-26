package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Picture-in-picture composition (media Phase 2).
 *
 * Lay an overlay clip over a base video for a timed window. Demonstrates the record-builder pattern
 * (`video_overlay → overlays_list → video_compose`) that assembles the list `video_compose` needs.
 */
internal val pictureInPictureWorkflow = WorkflowDefinition(
    id = "picture-in-picture", name = "Picture-in-Picture",
    nodes = listOf(
        guideNote(
            """
            Picture-in-Picture

            Overlays a second clip onto a base video for a time window.

            Flow: import base + overlay clips → Video Overlay (place + fade)
            → Overlays List → Video Compose → Video Encode → Media Output.
            Use cases: reaction cams, logos, callouts, watermarks.
            Tips: Video Overlay sets x/y, the start/end window, and opacity;
            add more layers via overlay2-4 on the Overlays List.
            """,
            height = 280,
        ),
        NodeRef("resolveBase", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.mp4"),
        )),
        NodeRef("resolveOverlay", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("clip1.mp4"),
        )),
        NodeRef("import_base", "media.video_import"),
        NodeRef("import_overlay", "media.video_import"),
        NodeRef("overlay", "media.video_overlay", config = mapOf(
            "x" to WorkflowValue.IntValue(24),
            "y" to WorkflowValue.IntValue(24),
            "start_ms" to WorkflowValue.DoubleValue(0.0),
            "end_ms" to WorkflowValue.DoubleValue(5_000.0),
            "opacity" to WorkflowValue.DoubleValue(0.85),
        )),
        NodeRef("overlays", "media.overlays_list"),
        NodeRef("compose", "media.video_compose"),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("composed.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolveBase", "resolved_path", "import_base", "path"),
        ConnectionRef("resolveOverlay", "resolved_path", "import_overlay", "path"),
        ConnectionRef("import_overlay", "video", "overlay", "source"),
        ConnectionRef("overlay", "overlay", "overlays", "overlay1"),
        ConnectionRef("import_base", "video", "compose", "base_video"),
        ConnectionRef("overlays", "overlays", "compose", "overlays"),
        ConnectionRef("compose", "video", "encode", "video"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)

/**
 * Audio/video sync calibration (media Phase 2).
 *
 * Measure drift at a couple of points and average it into delay offsets. Demonstrates the
 * `sync_point → sync_points_list → timing_controller` builder chain.
 */
internal val syncCalibrationWorkflow = WorkflowDefinition(
    id = "sync-calibration", name = "Sync Calibration",
    nodes = listOf(
        guideNote(
            """
            Sync Calibration

            Averages measured drift into video/audio/caption delays.

            Flow: import video → Sync Point ×N (source vs target ms) →
            Sync Points List → Timing Controller → Preview.
            Use cases: lip-sync fixes, caption offset, A/V realignment.
            Tips: each Sync Point pairs an observed source time with the
            target time it should land on; the controller averages them.
            """,
            height = 280,
        ),
        NodeRef("resolveVideo", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.mp4"),
        )),
        NodeRef("import_video", "media.video_import"),
        NodeRef("point1", "media.sync_point", config = mapOf(
            "source_ms" to WorkflowValue.DoubleValue(0.0),
            "target_ms" to WorkflowValue.DoubleValue(120.0),
        )),
        NodeRef("point2", "media.sync_point", config = mapOf(
            "source_ms" to WorkflowValue.DoubleValue(5_000.0),
            "target_ms" to WorkflowValue.DoubleValue(5_180.0),
        )),
        NodeRef("points", "media.sync_points_list"),
        NodeRef("timing", "media.timing_controller"),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("resolveVideo", "resolved_path", "import_video", "path"),
        ConnectionRef("import_video", "video", "timing", "base_video"),
        ConnectionRef("point1", "point", "points", "point1"),
        ConnectionRef("point2", "point", "points", "point2"),
        ConnectionRef("points", "sync_points", "timing", "sync_points"),
        ConnectionRef("timing", "config", "preview", "value"),
    ),
)
