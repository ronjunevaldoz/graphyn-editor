package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Shared record types for Phase 2 captioning and composition nodes.
 *
 * A caption segment carries its own timing so speech-to-text output can flow straight into
 * [MediaCompositionSpecs.captionOverlay] without an intermediate timing node.
 */
object MediaCompositionTypes {
    val captionSegment = WorkflowType.RecordType(
        mapOf(
            "text" to WorkflowType.StringType,
            "start_ms" to WorkflowType.DoubleType,
            "end_ms" to WorkflowType.DoubleType,
        ),
    )

    val captions = WorkflowType.ListType(captionSegment)

    /** One layer placed over a base video, optionally limited to a time window and faded by opacity. */
    val videoOverlay = WorkflowType.RecordType(
        mapOf(
            "source" to MediaTypes.videoHandle,
            "x" to WorkflowType.IntType,
            "y" to WorkflowType.IntType,
            "start_ms" to WorkflowType.DoubleType,
            "end_ms" to WorkflowType.DoubleType,
            "opacity" to WorkflowType.DoubleType,
        ),
    )

    val videoOverlays = WorkflowType.ListType(videoOverlay)

    /** A measured (source → target) time pair the timing controller averages into a delay. */
    val syncPoint = WorkflowType.RecordType(
        mapOf(
            "source_ms" to WorkflowType.DoubleType,
            "target_ms" to WorkflowType.DoubleType,
        ),
    )

    val syncPoints = WorkflowType.ListType(syncPoint)

    val timingConfig = WorkflowType.RecordType(
        mapOf(
            "video_delay_ms" to WorkflowType.DoubleType,
            "audio_delay_ms" to WorkflowType.DoubleType,
            "caption_offset_ms" to WorkflowType.DoubleType,
        ),
    )

    fun captionValue(text: String, startMs: Double, endMs: Double): WorkflowValue.RecordValue =
        WorkflowValue.RecordValue(
            mapOf(
                "text" to WorkflowValue.StringValue(text),
                "start_ms" to WorkflowValue.DoubleValue(startMs),
                "end_ms" to WorkflowValue.DoubleValue(endMs),
            ),
        )

    fun captionList(segments: List<Triple<String, Double, Double>>): WorkflowValue.ListValue =
        WorkflowValue.ListValue(segments.map { (text, start, end) -> captionValue(text, start, end) })

    fun timingConfigValue(videoDelayMs: Double, audioDelayMs: Double, captionOffsetMs: Double): WorkflowValue.RecordValue =
        WorkflowValue.RecordValue(
            mapOf(
                "video_delay_ms" to WorkflowValue.DoubleValue(videoDelayMs),
                "audio_delay_ms" to WorkflowValue.DoubleValue(audioDelayMs),
                "caption_offset_ms" to WorkflowValue.DoubleValue(captionOffsetMs),
            ),
        )
}
