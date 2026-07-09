package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.plugins.mediacore.model.Caption
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle
import com.ronjunevaldoz.graphyn.plugins.mediacore.renderer.AssCaptionRenderer
import com.ronjunevaldoz.graphyn.plugins.mediacore.renderer.CaptionRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.UUID

fun createMediaCoreBackend(): MediaCoreBackend {
    return FfmpegMediaCoreBackend()
}


/**
 * FFmpeg/FFprobe-backed [com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCoreBackend]. This class owns only process execution and temp files;
 * the per-operation command building lives in sibling `Ffmpeg*Impl` extension files so each file
 * stays within the size ceiling. Configure binaries via `GRAPHYN_FFMPEG` / `GRAPHYN_FFPROBE`.
 */
class FfmpegMediaCoreBackend(
    internal val ffmpeg: String = System.getenv("GRAPHYN_FFMPEG")?.takeIf(String::isNotBlank)
        ?: "ffmpeg",
    internal val ffprobe: String = System.getenv("GRAPHYN_FFPROBE")?.takeIf(String::isNotBlank)
        ?: "ffprobe",
    private val tempDirectory: File = File(GraphynMediaPaths.temp()),
) : MediaCoreBackend {
    val ass = AssCaptionRenderer()
    override suspend fun inspectVideo(path: String) = inspectVideoImpl(path)
    override suspend fun inspectImage(path: String) = inspectImageImpl(path)
    override suspend fun extractAudio(videoPath: String) = extractAudioImpl(videoPath)
    override suspend fun mixAudio(audioPaths: List<String>, volumes: List<Double>) =
        mixAudioImpl(audioPaths, volumes)

    override suspend fun stitchVideos(videoPaths: List<String>) = stitchVideosImpl(videoPaths)

    override suspend fun encodeVideo(
        videoPath: String,
        audioPath: String?,
        outputPath: String,
        bitrate: String,
        codec: String,
    ) = encodeVideoImpl(videoPath, audioPath, outputPath, bitrate, codec)

    override suspend fun overlayCaptions(
        videoPath: String,
        captions: List<Caption>,
        style: CaptionStyle
    ) =
        renderCaptionOverlay(captionRenderer = ass, videoPath, captions, style)

    override suspend fun composeVideo(baseVideoPath: String, overlays: List<VideoOverlay>) =
        renderVideoCompose(baseVideoPath, overlays)

    override suspend fun encodeAudio(audioPath: String, outputPath: String, format: String) =
        encodeAudioImpl(audioPath, outputPath, format)

    override suspend fun resizeImage(imagePath: String, width: Int, height: Int) =
        resizeImageImpl(imagePath, width, height)

    override suspend fun cropImage(imagePath: String, x: Int, y: Int, width: Int, height: Int) =
        cropImageImpl(imagePath, x, y, width, height)

    override suspend fun imageSequenceToVideo(imagePaths: List<String>, fps: Double) =
        imageSequenceToVideoImpl(imagePaths, fps)

    override suspend fun kenBurns(
        imagePath: String,
        durationMs: Double,
        fps: Double,
        zoomStart: Double,
        zoomEnd: Double,
        panX: String,
        panY: String,
        width: Int,
        height: Int,
    ) = kenBurnsImpl(imagePath, durationMs, fps, zoomStart, zoomEnd, panX, panY, width, height)

    override suspend fun compositeComparisonLayout(
        imageAPath: String,
        imageBPath: String,
        labelA: String,
        labelB: String,
        caption: String,
        mascotPath: String,
        style: ComparisonLayoutStyle,
        width: Int,
        height: Int,
    ) = compositeComparisonLayoutImpl(imageAPath, imageBPath, labelA, labelB, caption, mascotPath, style, width, height)

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
            "${command.first()} failed with exit code $exitCode: ${
                output.trim().ifEmpty { "unknown error" }
            }"
        }
        output
    }

    internal fun tempFile(prefix: String, extension: String): File {
        tempDirectory.mkdirs()
        return File(tempDirectory, "$prefix-${UUID.randomUUID()}.$extension")
    }

    private suspend fun MediaCoreBackend.renderVideoCompose(
        baseVideoPath: String,
        overlays: List<VideoOverlay>,
    ): VideoMetadata {
        require(overlays.isNotEmpty()) { "Video Compose requires at least one overlay." }
        val base = requireFile(baseVideoPath, "Base video")
        val sources = overlays.map { requireFile(it.sourcePath, "Overlay video") }
        val output = tempFile("video-compose", "mp4")

        val command = mutableListOf(ffmpeg, "-v", "error", "-y", "-i", base.absolutePath)
        sources.forEach { command += listOf("-i", it.absolutePath) }
        command += listOf(
            "-filter_complex",
            buildComposeFilter(overlays),
            "-map",
            "[v${overlays.size}]"
        )
        command += listOf(
            "-map",
            "0:a?",
            "-c:a",
            "copy",
            "-movflags",
            "+faststart",
            output.absolutePath
        )
        run(*command.toTypedArray())
        return inspectVideo(output.absolutePath)
    }

    private fun buildComposeFilter(overlays: List<VideoOverlay>): String = buildString {
        overlays.forEachIndexed { index, overlay ->
            require(overlay.opacity in 0.0..1.0) { "Overlay opacity must be between 0.0 and 1.0." }
            require(overlay.endMs >= overlay.startMs) { "Overlay end_ms must be >= start_ms." }
            val input = index + 1
            val prev = if (index == 0) "0:v" else "v$index"
            append("[$input:v]format=yuva420p,colorchannelmixer=aa=${overlay.opacity}[o$input];")
            append("[$prev][o$input]overlay=x=${overlay.x}:y=${overlay.y}")
            append(":enable='between(t,${overlay.startMs / 1000.0},${overlay.endMs / 1000.0})'")
            append("[v${index + 1}]")
            if (index < overlays.lastIndex) append(';')
        }
    }


    /**
     * FFmpeg implementations for the Phase 2 composition nodes. Kept as extension functions on
     * [com.ronjunevaldoz.graphyn.plugins.mediacore.ffmpeg.FfmpegMediaCoreBackend] so the heavy filter/ASS string building lives apart from the Phase 1
     * decode/encode methods and each file stays within the size ceiling.
     */
    internal suspend fun MediaCoreBackend.renderCaptionOverlay(
        captionRenderer: CaptionRenderer<String>,
        videoPath: String,
        captions: List<Caption>,
        style: CaptionStyle,
    ): VideoMetadata {
        require(captions.isNotEmpty()) { "Caption Overlay requires at least one caption." }
        check(supportsFilter("ass")) {
            "Caption Overlay requires FFmpeg built with libass (the 'ass' filter)."
        }
        val source = requireFile(videoPath, "Video")
        val metadata = inspectVideo(source.absolutePath)
        val assFile = tempFile("captions", "ass")
        val output = tempFile("caption-overlay", "mp4")

        try {
            val ass = captionRenderer.render(
                captions,
                style,
                metadata.width,
                metadata.height,
            )
            assFile.writeText(ass)

            run(
                ffmpeg, "-v", "error", "-y",
                "-i", source.absolutePath,
                "-vf", "ass='${assFile.absolutePath.escapeFilterPath()}'",
                "-c:a", "copy",
                "-movflags", "+faststart",
                output.absolutePath,
            )
        } finally {
            assFile.delete()
        }
        return inspectVideo(output.absolutePath)
    }


    private fun String.escapeFilterPath(): String =
        replace("\\", "/").replace(":", "\\:").replace("'", "\\'")

}

internal fun requireFile(path: String, label: String): File {
    require(path.isNotBlank()) { "$label path must not be blank." }
    return File(path).absoluteFile.also {
        require(it.isFile) { "$label file does not exist: ${it.absolutePath}" }
    }
}