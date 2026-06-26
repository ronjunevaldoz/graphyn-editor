package com.ronjunevaldoz.graphyn.plugins.mediacore

import kotlin.math.roundToInt
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Probe/decode operations for [FfmpegMediaCoreBackend]; see that class for the process plumbing. */
internal suspend fun FfmpegMediaCoreBackend.inspectVideoImpl(path: String): VideoMetadata {
    val source = requireFile(path, "Video")
    val root = probe(
        "-select_streams", "v:0",
        "-show_entries", "stream=width,height,r_frame_rate,nb_frames:format=duration",
        source.absolutePath,
    )
    val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
        ?: error("No video stream found in ${source.absolutePath}.")
    val durationMs = root.formatDurationMs()
    val fps = stream.probeString("r_frame_rate").toFps()
    val frameCount = stream.probeStringOrNull("nb_frames")?.toIntOrNull()
        ?: (durationMs / 1000.0 * fps).roundToInt()
    return VideoMetadata(
        path = source.absolutePath,
        width = stream.probeInt("width"),
        height = stream.probeInt("height"),
        durationMs = durationMs,
        fps = fps,
        frameCount = frameCount,
    )
}

internal suspend fun FfmpegMediaCoreBackend.inspectImageImpl(path: String): ImageMetadata {
    val source = requireFile(path, "Image")
    val root = probe("-select_streams", "v:0", "-show_entries", "stream=width,height", source.absolutePath)
    val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
        ?: error("No image stream found in ${source.absolutePath}.")
    return ImageMetadata(path = source.absolutePath, width = stream.probeInt("width"), height = stream.probeInt("height"))
}

internal suspend fun FfmpegMediaCoreBackend.extractAudioImpl(videoPath: String): AudioMetadata {
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

internal suspend fun FfmpegMediaCoreBackend.inspectAudioImpl(path: String): AudioMetadata {
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
        sampleRate = stream.probeInt("sample_rate"),
        durationMs = root.formatDurationMs(),
    )
}

internal fun JsonObject.formatDurationMs(): Double =
    this["format"]?.jsonObject?.probeString("duration")?.toDoubleOrNull()?.times(1000.0)
        ?: error("ffprobe did not return media duration.")

internal fun JsonObject.probeString(key: String): String =
    this[key]?.jsonPrimitive?.content ?: error("ffprobe did not return '$key'.")

internal fun JsonObject.probeStringOrNull(key: String): String? = this[key]?.jsonPrimitive?.content

internal fun JsonObject.probeInt(key: String): Int =
    probeString(key).toIntOrNull() ?: error("ffprobe returned an invalid '$key' value.")

internal fun String.toFps(): Double {
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
