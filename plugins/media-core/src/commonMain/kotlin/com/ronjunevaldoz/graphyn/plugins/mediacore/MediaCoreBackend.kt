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

    suspend fun imageSequenceToVideo(imagePaths: List<String>, fps: Double): VideoMetadata
}

expect fun createMediaCoreBackend() : MediaCoreBackend

