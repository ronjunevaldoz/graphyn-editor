package com.ronjunevaldoz.graphyn.plugins.mediacore

import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FfmpegMediaCoreBackend(
    internal val ffmpeg: String = System.getenv("GRAPHYN_FFMPEG")?.takeIf(String::isNotBlank) ?: "ffmpeg",
    private val ffprobe: String = System.getenv("GRAPHYN_FFPROBE")?.takeIf(String::isNotBlank) ?: "ffprobe",
    private val tempDirectory: File = GraphynMediaPaths.temp(),
) : MediaCoreBackend {
    override suspend fun inspectVideo(path: String): VideoMetadata {
        val source = requireFile(path, "Video")
        val root = probe(
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height,r_frame_rate,nb_frames:format=duration",
            source.absolutePath,
        )
        val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("No video stream found in ${source.absolutePath}.")
        val durationMs = root.formatDurationMs()
        val fps = stream.string("r_frame_rate").toFps()
        val frameCount = stream.stringOrNull("nb_frames")?.toIntOrNull()
            ?: (durationMs / 1000.0 * fps).roundToInt()
        return VideoMetadata(
            path = source.absolutePath,
            width = stream.int("width"),
            height = stream.int("height"),
            durationMs = durationMs,
            fps = fps,
            frameCount = frameCount,
        )
    }

    override suspend fun inspectImage(path: String): ImageMetadata {
        val source = requireFile(path, "Image")
        val root = probe(
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height",
            source.absolutePath,
        )
        val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("No image stream found in ${source.absolutePath}.")
        return ImageMetadata(path = source.absolutePath, width = stream.int("width"), height = stream.int("height"))
    }

    override suspend fun extractAudio(videoPath: String): AudioMetadata {
        val source = requireFile(videoPath, "Video")
        val output = tempFile("audio-extract", "wav")
        run(
            ffmpeg, "-v", "error", "-y",
            "-i", source.absolutePath,
            "-map", "0:a:0",
            "-c:a", "pcm_s16le",
            output.absolutePath,
        )
        return inspectAudio(output.absolutePath)
    }

    override suspend fun mixAudio(audioPaths: List<String>, volumes: List<Double>): AudioMetadata {
        require(audioPaths.isNotEmpty()) { "Audio Mix requires at least one audio track." }
        require(volumes.isEmpty() || volumes.size == audioPaths.size) {
            "Audio Mix requires one volume per track."
        }
        val sources = audioPaths.map { requireFile(it, "Audio") }
        val resolvedVolumes = if (volumes.isEmpty()) List(sources.size) { 1.0 } else volumes
        resolvedVolumes.forEach { require(it in 0.0..1.0) { "Audio volumes must be between 0.0 and 1.0." } }

        val output = tempFile("audio-mix", "wav")
        val command = mutableListOf(ffmpeg, "-v", "error", "-y")
        sources.forEach { command += listOf("-i", it.absolutePath) }
        val filters = buildString {
            resolvedVolumes.forEachIndexed { index, volume ->
                append("[$index:a]volume=$volume[a$index];")
            }
            resolvedVolumes.indices.forEach { append("[a$it]") }
            append("amix=inputs=${sources.size}:duration=longest:normalize=0[aout]")
        }
        command += listOf(
            "-filter_complex", filters,
            "-map", "[aout]",
            "-c:a", "pcm_s16le",
            output.absolutePath,
        )
        run(*command.toTypedArray())
        return inspectAudio(output.absolutePath)
    }

    override suspend fun stitchVideos(videoPaths: List<String>): VideoMetadata {
        require(videoPaths.isNotEmpty()) { "Video Stitch requires at least one video clip." }
        val sources = videoPaths.map { requireFile(it, "Video") }
        val concatFile = tempFile("video-concat", "txt")
        concatFile.writeText(sources.joinToString("\n") { "file '${it.absolutePath.escapeConcatPath()}'" })
        val output = tempFile("video-stitch", "mp4")
        run(
            ffmpeg, "-v", "error", "-y",
            "-f", "concat",
            "-safe", "0",
            "-i", concatFile.absolutePath,
            "-c", "copy",
            "-movflags", "+faststart",
            output.absolutePath,
        )
        return inspectVideo(output.absolutePath)
    }

    override suspend fun encodeVideo(
        videoPath: String,
        audioPath: String?,
        outputPath: String,
        bitrate: String,
        codec: String,
    ): EncodedVideo {
        require(codec == "h264") { "Unsupported video codec '$codec'." }
        val video = requireFile(videoPath, "Video")
        val output = File(outputPath).absoluteFile
        require(output.name.endsWith(".mp4", ignoreCase = true)) { "Video Encode output_path must end in .mp4." }
        output.parentFile?.mkdirs()

        val command = mutableListOf(
            ffmpeg, "-v", "error", "-y",
            "-i", video.absolutePath,
        )
        if (audioPath != null) {
            command += listOf("-i", requireFile(audioPath, "Audio").absolutePath)
        }
        command += listOf(
            "-map", "0:v:0",
            "-c:v", "libx264",
            "-b:v", bitrate.toFfmpegBitrate(),
            "-pix_fmt", "yuv420p",
        )
        if (audioPath != null) {
            command += listOf("-map", "1:a:0", "-c:a", "aac", "-shortest")
        } else {
            command += "-an"
        }
        command += listOf("-movflags", "+faststart", output.absolutePath)
        run(*command.toTypedArray())

        val metadata = inspectVideo(output.absolutePath)
        return EncodedVideo(
            path = output.absolutePath,
            sizeBytes = output.length(),
            durationMs = metadata.durationMs,
        )
    }

    override suspend fun overlayCaptions(
        videoPath: String,
        captions: List<Caption>,
        style: CaptionStyle,
    ): VideoMetadata = renderCaptionOverlay(videoPath, captions, style)

    override suspend fun composeVideo(
        baseVideoPath: String,
        overlays: List<VideoOverlay>,
    ): VideoMetadata = renderVideoCompose(baseVideoPath, overlays)

    suspend fun inspectAudio(path: String): AudioMetadata {
        val source = requireFile(path, "Audio")
        val root = probe(
            "-select_streams", "a:0",
            "-show_entries", "stream=sample_rate:format=duration",
            source.absolutePath,
        )
        val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("No audio stream found in ${source.absolutePath}.")
        return AudioMetadata(
            path = source.absolutePath,
            sampleRate = stream.int("sample_rate"),
            durationMs = root.formatDurationMs(),
        )
    }

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

    private suspend fun probe(vararg arguments: String): JsonObject {
        val result = run(
            ffprobe, "-v", "error",
            "-of", "json",
            *arguments,
        )
        return Json.parseToJsonElement(result).jsonObject
    }

    internal suspend fun run(vararg command: String): String = withContext(Dispatchers.IO) {
        val process = try {
            ProcessBuilder(command.toList())
                .redirectErrorStream(true)
                .start()
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

private fun JsonObject.formatDurationMs(): Double =
    this["format"]?.jsonObject?.string("duration")?.toDoubleOrNull()?.times(1000.0)
        ?: error("ffprobe did not return media duration.")

private fun JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.content ?: error("ffprobe did not return '$key'.")

private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.content

private fun JsonObject.int(key: String): Int =
    string(key).toIntOrNull() ?: error("ffprobe returned an invalid '$key' value.")

private fun String.toFps(): Double {
    val parts = split("/")
    return when (parts.size) {
        1 -> toDoubleOrNull()
        2 -> {
            val numerator = parts[0].toDoubleOrNull()
            val denominator = parts[1].toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) numerator / denominator else null
        }
        else -> null
    } ?: error("ffprobe returned an invalid frame rate '$this'.")
}

private fun String.toFfmpegBitrate(): String = when (this) {
    "low" -> "1M"
    "medium" -> "4M"
    "high" -> "8M"
    else -> error("Unsupported bitrate '$this'.")
}

private fun String.escapeConcatPath(): String = replace("'", "'\\''")
