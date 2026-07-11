package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.plugins.mediacore.model.Caption
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle

data class VideoMetadata(
    val path: String,
    val width: Int,
    val height: Int,
    val durationMs: Double,
    val fps: Double,
    val frameCount: Int,
)

data class AudioMetadata(
    val path: String,
    val sampleRate: Int,
    val durationMs: Double,
)

data class EncodedVideo(
    val path: String,
    val sizeBytes: Long,
    val durationMs: Double,
)

data class ImageMetadata(
    val path: String,
    val width: Int,
    val height: Int,
)

data class EncodedAudio(
    val path: String,
    val sizeBytes: Long,
    val durationMs: Double,
)

/** One overlay layer placed over a base video, in pixels and milliseconds. */
data class VideoOverlay(
    val sourcePath: String,
    val x: Int,
    val y: Int,
    val startMs: Double,
    val endMs: Double,
    val opacity: Double,
)

/** Appearance for [MediaCoreBackend.compositeComparisonLayout] — colors are hex strings ("#RRGGBB"). */
data class ComparisonLayoutStyle(
    val backgroundColor: String = "#FFFFFF",
    val labelFontFamily: String = "Arial",
    val labelFontSize: Int = 36,
    val labelColor: String = "#000000",
    val captionFontFamily: String = "Arial",
    val captionFontSize: Int = 44,
    val captionColor: String = "#000000",
    val panelGap: Int = 24,
)

interface MediaCoreBackend {
    suspend fun inspectVideo(path: String): VideoMetadata
    suspend fun inspectImage(path: String): ImageMetadata
    suspend fun extractAudio(videoPath: String): AudioMetadata
    suspend fun mixAudio(audioPaths: List<String>, volumes: List<Double>): AudioMetadata
    suspend fun stitchVideos(videoPaths: List<String>): VideoMetadata
    suspend fun encodeVideo(
        videoPath: String,
        audioPath: String?,
        outputPath: String,
        bitrate: String,
        codec: String,
    ): EncodedVideo

    suspend fun overlayCaptions(
        videoPath: String,
        captions: List<Caption>,
        style: CaptionStyle,
    ): VideoMetadata

    suspend fun composeVideo(
        baseVideoPath: String,
        overlays: List<VideoOverlay>,
    ): VideoMetadata

    suspend fun encodeAudio(
        audioPath: String,
        outputPath: String,
        format: String,
    ): EncodedAudio

    suspend fun resizeImage(imagePath: String, width: Int, height: Int): ImageMetadata

    suspend fun cropImage(imagePath: String, x: Int, y: Int, width: Int, height: Int): ImageMetadata

    /** Mirrors an image horizontally — e.g. turning a left-pointing character into a right-pointing
     * one without a second AI generation call. */
    suspend fun flipImage(imagePath: String): ImageMetadata

    suspend fun imageSequenceToVideo(imagePaths: List<String>, fps: Double): VideoMetadata

    /** Renders a still image into a video with a slow pan/zoom (Ken Burns effect) instead of a static repeat. */
    suspend fun kenBurns(
        imagePath: String,
        durationMs: Double,
        fps: Double,
        zoomStart: Double,
        zoomEnd: Double,
        panX: String,
        panY: String,
        width: Int,
        height: Int,
    ): VideoMetadata

    /**
     * Composites two labeled images side-by-side, a caption, and a mascot image into one still
     * frame — the "X vs Y" comparison-explainer layout. Pure layout/rendering, no AI model
     * involved; [imageAPath]/[imageBPath]/[mascotPath] are caller-supplied (AI-generated or not).
     */
    suspend fun compositeComparisonLayout(
        imageAPath: String,
        imageBPath: String,
        labelA: String,
        labelB: String,
        caption: String,
        mascotPath: String,
        style: ComparisonLayoutStyle,
        width: Int,
        height: Int,
    ): ImageMetadata
}

