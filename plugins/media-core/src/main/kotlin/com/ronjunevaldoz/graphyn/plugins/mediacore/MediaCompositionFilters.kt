package com.ronjunevaldoz.graphyn.plugins.mediacore

/**
 * FFmpeg implementations for the Phase 2 composition nodes. Kept as extension functions on
 * [FfmpegMediaCoreBackend] so the heavy filter/ASS string building lives apart from the Phase 1
 * decode/encode methods and each file stays within the size ceiling.
 */
internal suspend fun FfmpegMediaCoreBackend.renderCaptionOverlay(
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
    assFile.writeText(buildAssDocument(captions, style, metadata.width, metadata.height))
    val output = tempFile("caption-overlay", "mp4")
    run(
        ffmpeg, "-v", "error", "-y",
        "-i", source.absolutePath,
        "-vf", "ass=${assFile.absolutePath.escapeFilterPath()}",
        "-c:a", "copy",
        "-movflags", "+faststart",
        output.absolutePath,
    )
    return inspectVideo(output.absolutePath)
}

internal suspend fun FfmpegMediaCoreBackend.renderVideoCompose(
    baseVideoPath: String,
    overlays: List<VideoOverlay>,
): VideoMetadata {
    require(overlays.isNotEmpty()) { "Video Compose requires at least one overlay." }
    val base = requireFile(baseVideoPath, "Base video")
    val sources = overlays.map { requireFile(it.sourcePath, "Overlay video") }
    val output = tempFile("video-compose", "mp4")

    val command = mutableListOf(ffmpeg, "-v", "error", "-y", "-i", base.absolutePath)
    sources.forEach { command += listOf("-i", it.absolutePath) }
    command += listOf("-filter_complex", buildComposeFilter(overlays), "-map", "[v${overlays.size}]")
    command += listOf("-map", "0:a?", "-c:a", "copy", "-movflags", "+faststart", output.absolutePath)
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

private fun buildAssDocument(captions: List<Caption>, style: CaptionStyle, width: Int, height: Int): String {
    val primary = style.color.toAssColor()
    val back = style.backgroundColor.toAssColor()
    val alignment = when (style.position) {
        "top" -> 8
        "center" -> 5
        else -> 2
    }
    val events = captions.joinToString("\n") { caption ->
        require(caption.endMs >= caption.startMs) { "Caption end_ms must be >= start_ms." }
        "Dialogue: 0,${caption.startMs.toAssTime()},${caption.endMs.toAssTime()},Default,,0,0,0,,${caption.text.escapeAssText()}"
    }
    return """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: $width
        PlayResY: $height

        [V4+ Styles]
        Format: Name, Fontsize, PrimaryColour, BackColour, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV
        Style: Default,${style.fontSize},$primary,$back,3,0,0,$alignment,20,20,40

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        $events
    """.trimIndent() + "\n"
}

/** Converts `#RRGGBB` / `#AARRGGBB` to ASS `&HAABBGGRR`. ASS alpha is inverted (00 = opaque). */
private fun String.toAssColor(): String {
    require(startsWith("#") && (length == 7 || length == 9)) { "Caption colors must use #RRGGBB or #AARRGGBB." }
    val hex = drop(1)
    val (a, r, g, b) = if (hex.length == 8) {
        listOf(hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6), hex.substring(6, 8))
    } else {
        listOf("FF", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6))
    }
    val assAlpha = (255 - a.toInt(16)).toString(16).padStart(2, '0').uppercase()
    return "&H$assAlpha$b$g$r"
}

private fun Double.toAssTime(): String {
    val totalCentis = (this / 10.0).toLong().coerceAtLeast(0)
    val centis = totalCentis % 100
    val totalSeconds = totalCentis / 100
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
}

private fun String.escapeAssText(): String = replace("\n", "\\N").replace("{", "(").replace("}", ")")

private fun String.escapeFilterPath(): String = replace("\\", "/").replace(":", "\\:").replace("'", "\\'")

private operator fun <T> List<T>.component4(): T = this[3]
