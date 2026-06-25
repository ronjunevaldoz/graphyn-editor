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

interface MediaCoreBackend {
    suspend fun inspectVideo(path: String): VideoMetadata
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
}

internal object GraphynMediaPaths {
    fun root(): File {
        val configured = System.getenv("GRAPHYN_HOME")?.takeIf(String::isNotBlank)
        return File(configured ?: File(System.getProperty("user.home"), ".graphyn").path)
    }

    fun temp(): File = File(root(), "temp").apply { mkdirs() }
    fun ttsCache(): File = File(root(), "cache/tts").apply { mkdirs() }
}
