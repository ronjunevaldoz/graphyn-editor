package com.ronjunevaldoz.graphyn.plugins.mediacore

import java.io.File

/** Mix/stitch/encode operations for [FfmpegMediaCoreBackend]; see that class for process plumbing. */
internal suspend fun FfmpegMediaCoreBackend.mixAudioImpl(audioPaths: List<String>, volumes: List<Double>): AudioMetadata {
    require(audioPaths.isNotEmpty()) { "Audio Mix requires at least one audio track." }
    require(volumes.isEmpty() || volumes.size == audioPaths.size) { "Audio Mix requires one volume per track." }
    val sources = audioPaths.map { requireFile(it, "Audio") }
    val resolvedVolumes = if (volumes.isEmpty()) List(sources.size) { 1.0 } else volumes
    resolvedVolumes.forEach { require(it in 0.0..1.0) { "Audio volumes must be between 0.0 and 1.0." } }

    val output = tempFile("audio-mix", "wav")
    val command = mutableListOf(ffmpeg, "-v", "error", "-y")
    sources.forEach { command += listOf("-i", it.absolutePath) }
    val filters = buildString {
        resolvedVolumes.forEachIndexed { index, volume -> append("[$index:a]volume=$volume[a$index];") }
        resolvedVolumes.indices.forEach { append("[a$it]") }
        append("amix=inputs=${sources.size}:duration=longest:normalize=0[aout]")
    }
    command += listOf("-filter_complex", filters, "-map", "[aout]", "-c:a", "pcm_s16le", output.absolutePath)
    run(*command.toTypedArray())
    return inspectAudio(output.absolutePath)
}

internal suspend fun FfmpegMediaCoreBackend.stitchVideosImpl(videoPaths: List<String>): VideoMetadata {
    require(videoPaths.isNotEmpty()) { "Video Stitch requires at least one video clip." }
    val sources = videoPaths.map { requireFile(it, "Video") }
    val concatFile = tempFile("video-concat", "txt")
    concatFile.writeText(sources.joinToString("\n") { "file '${it.absolutePath.escapeConcatPath()}'" })
    val output = tempFile("video-stitch", "mp4")
    run(
        ffmpeg, "-v", "error", "-y",
        "-f", "concat", "-safe", "0",
        "-i", concatFile.absolutePath,
        "-c", "copy",
        "-movflags", "+faststart",
        output.absolutePath,
    )
    return inspectVideo(output.absolutePath)
}

internal suspend fun FfmpegMediaCoreBackend.encodeVideoImpl(
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

    val command = mutableListOf(ffmpeg, "-v", "error", "-y", "-i", video.absolutePath)
    if (audioPath != null) command += listOf("-i", requireFile(audioPath, "Audio").absolutePath)
    command += listOf("-map", "0:v:0", "-c:v", "libx264", "-b:v", bitrate.toFfmpegBitrate(), "-pix_fmt", "yuv420p")
    if (audioPath != null) command += listOf("-map", "1:a:0", "-c:a", "aac", "-shortest") else command += "-an"
    command += listOf("-movflags", "+faststart", output.absolutePath)
    run(*command.toTypedArray())

    val metadata = inspectVideo(output.absolutePath)
    return EncodedVideo(path = output.absolutePath, sizeBytes = output.length(), durationMs = metadata.durationMs)
}

private fun String.toFfmpegBitrate(): String = when (this) {
    "low" -> "1M"
    "medium" -> "4M"
    "high" -> "8M"
    else -> error("Unsupported bitrate '$this'.")
}

private fun String.escapeConcatPath(): String = replace("'", "'\\''")
