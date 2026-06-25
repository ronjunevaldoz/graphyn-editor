package com.ronjunevaldoz.graphyn.plugins.mediacore

import java.io.File

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

/** A single timed caption line, in milliseconds relative to the start of the video. */
data class Caption(
    val text: String,
    val startMs: Double,
    val endMs: Double,
)

/** Resolved caption appearance passed to the backend when burning subtitles in. */
data class CaptionStyle(
    val color: String,
    val backgroundColor: String,
    val fontSize: Int,
    val position: String,
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
}

internal object GraphynMediaPaths {
    fun root(): File {
        val configured = System.getenv("GRAPHYN_HOME")?.takeIf(String::isNotBlank)
        return File(configured ?: File(System.getProperty("user.home"), ".graphyn").path)
    }

    fun temp(): File = File(root(), "temp").apply { mkdirs() }
    fun ttsCache(): File = File(root(), "cache/tts").apply { mkdirs() }
}
