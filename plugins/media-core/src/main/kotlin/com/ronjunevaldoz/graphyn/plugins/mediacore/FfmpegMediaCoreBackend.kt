package com.ronjunevaldoz.graphyn.plugins.mediacore

import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * FFmpeg/FFprobe-backed [MediaCoreBackend]. This class owns only process execution and temp files;
 * the per-operation command building lives in sibling `Ffmpeg*Impl` extension files so each file
 * stays within the size ceiling. Configure binaries via `GRAPHYN_FFMPEG` / `GRAPHYN_FFPROBE`.
 */
class FfmpegMediaCoreBackend(
    internal val ffmpeg: String = System.getenv("GRAPHYN_FFMPEG")?.takeIf(String::isNotBlank) ?: "ffmpeg",
    internal val ffprobe: String = System.getenv("GRAPHYN_FFPROBE")?.takeIf(String::isNotBlank) ?: "ffprobe",
    private val tempDirectory: File = GraphynMediaPaths.temp(),
) : MediaCoreBackend {
    override suspend fun inspectVideo(path: String) = inspectVideoImpl(path)
    override suspend fun inspectImage(path: String) = inspectImageImpl(path)
    override suspend fun extractAudio(videoPath: String) = extractAudioImpl(videoPath)
    override suspend fun mixAudio(audioPaths: List<String>, volumes: List<Double>) = mixAudioImpl(audioPaths, volumes)
    override suspend fun stitchVideos(videoPaths: List<String>) = stitchVideosImpl(videoPaths)

    override suspend fun encodeVideo(
        videoPath: String,
        audioPath: String?,
        outputPath: String,
        bitrate: String,
        codec: String,
    ) = encodeVideoImpl(videoPath, audioPath, outputPath, bitrate, codec)

    override suspend fun overlayCaptions(videoPath: String, captions: List<Caption>, style: CaptionStyle) =
        renderCaptionOverlay(videoPath, captions, style)

    override suspend fun composeVideo(baseVideoPath: String, overlays: List<VideoOverlay>) =
        renderVideoCompose(baseVideoPath, overlays)

    /** Public: reused by the TTS cache to read generated audio metadata. */
    suspend fun inspectAudio(path: String) = inspectAudioImpl(path)

    /** True when this FFmpeg build exposes [name] in `-filters` (e.g. `ass`, which needs libass). */
    internal suspend fun supportsFilter(name: String): Boolean = try {
        run(ffmpeg, "-hide_banner", "-filters").lineSequence().any { it.contains(" $name ") }
    } catch (_: Exception) {
        false
    }

    fun isAvailable(): Boolean = try {
        ProcessBuilder(ffmpeg, "-version").redirectErrorStream(true).start().waitFor() == 0 &&
            ProcessBuilder(ffprobe, "-version").redirectErrorStream(true).start().waitFor() == 0
    } catch (_: Exception) {
        false
    }

    internal suspend fun probe(vararg arguments: String): JsonObject {
        val result = run(ffprobe, "-v", "error", "-of", "json", *arguments)
        return Json.parseToJsonElement(result).jsonObject
    }

    internal suspend fun run(vararg command: String): String = withContext(Dispatchers.IO) {
        val process = try {
            ProcessBuilder(command.toList()).redirectErrorStream(true).start()
        } catch (error: Exception) {
            error(
                "Unable to start '${command.first()}'. Install FFmpeg or configure " +
                    "GRAPHYN_FFMPEG and GRAPHYN_FFPROBE. ${error.message}",
            )
        }
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "${command.first()} failed with exit code $exitCode: ${output.trim().ifEmpty { "unknown error" }}"
        }
        output
    }

    internal fun tempFile(prefix: String, extension: String): File {
        tempDirectory.mkdirs()
        return File(tempDirectory, "$prefix-${UUID.randomUUID()}.$extension")
    }
}

internal fun requireFile(path: String, label: String): File {
    require(path.isNotBlank()) { "$label path must not be blank." }
    return File(path).absoluteFile.also {
        require(it.isFile) { "$label file does not exist: ${it.absolutePath}" }
    }
}
