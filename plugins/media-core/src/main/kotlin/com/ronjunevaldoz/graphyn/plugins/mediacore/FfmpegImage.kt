package com.ronjunevaldoz.graphyn.plugins.mediacore

/** Phase 3 image operations for [com.ronjunevaldoz.graphyn.plugins.mediacore.ffmpeg.FfmpegMediaCoreBackend]; see that class for the process plumbing. */
internal suspend fun FfmpegMediaCoreBackend.resizeImageImpl(
    imagePath: String,
    width: Int,
    height: Int
): ImageMetadata {
    require(width > 0 && height > 0) { "Image Resize dimensions must be positive." }
    val source = requireFile(imagePath, "Image")
    val output = tempFile("image-resize", "png")
    run(
        ffmpeg,
        "-v",
        "error",
        "-y",
        "-i",
        source.absolutePath,
        "-vf",
        "scale=$width:$height",
        "-frames:v",
        "1",
        output.absolutePath
    )
    return inspectImage(output.absolutePath)
}

internal suspend fun FfmpegMediaCoreBackend.cropImageImpl(
    imagePath: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
): ImageMetadata {
    require(width > 0 && height > 0) { "Image Crop dimensions must be positive." }
    require(x >= 0 && y >= 0) { "Image Crop origin must be non-negative." }
    val source = requireFile(imagePath, "Image")
    val output = tempFile("image-crop", "png")
    run(
        ffmpeg,
        "-v",
        "error",
        "-y",
        "-i",
        source.absolutePath,
        "-vf",
        "crop=$width:$height:$x:$y",
        "-frames:v",
        "1",
        output.absolutePath
    )
    return inspectImage(output.absolutePath)
}

internal suspend fun FfmpegMediaCoreBackend.imageSequenceToVideoImpl(
    imagePaths: List<String>,
    fps: Double
): VideoMetadata {
    require(imagePaths.isNotEmpty()) { "Image Sequence requires at least one image." }
    require(fps > 0.0) { "Image Sequence fps must be positive." }
    val sources = imagePaths.map { requireFile(it, "Image") }
    val perFrame = 1.0 / fps
    val concat = tempFile("image-seq", "txt")
    concat.writeText(
        buildString {
            sources.forEach { append("file '${it.absolutePath.escapeConcat()}'\nduration $perFrame\n") }
            // The concat demuxer ignores the final entry's duration, so repeat the last frame.
            append("file '${sources.last().absolutePath.escapeConcat()}'\n")
        },
    )
    val output = tempFile("image-seq", "mp4")
    run(
        ffmpeg, "-v", "error", "-y",
        "-f", "concat", "-safe", "0",
        "-i", concat.absolutePath,
        "-vf", "fps=$fps,format=yuv420p",
        "-c:v", "libx264",
        "-movflags", "+faststart",
        output.absolutePath,
    )
    return inspectVideo(output.absolutePath)
}

private fun String.escapeConcat(): String = replace("'", "'\\''")

/**
 * A single still frame, not animated — unlike [kenBurnsImpl] there's no per-frame expression math,
 * so panel/label/caption/mascot positions are precomputed once in Kotlin as plain pixel values.
 * Panels are proportional to [width]/[height] (not hardcoded absolute pixels) so this stays sane
 * at resolutions other than the 720x1280 default.
 */
internal suspend fun FfmpegMediaCoreBackend.compositeComparisonLayoutImpl(
    imageAPath: String,
    imageBPath: String,
    labelA: String,
    labelB: String,
    caption: String,
    mascotPath: String,
    style: ComparisonLayoutStyle,
    width: Int,
    height: Int,
): ImageMetadata {
    require(width > 0 && height > 0) { "Comparison Layout dimensions must be positive." }
    val sourceA = requireFile(imageAPath, "Image A")
    val sourceB = requireFile(imageBPath, "Image B")
    val mascot = requireFile(mascotPath, "Mascot image")

    val margin = (width * 0.06).toInt()
    val panelGap = style.panelGap
    val panelW = (width - 2 * margin - panelGap) / 2
    val panelH = (height * 0.30).toInt()
    val panelY = (height * 0.09).toInt()
    val labelY = (height * 0.03).toInt()
    val panelAX = margin
    val panelBX = margin + panelW + panelGap
    val captionY = panelY + panelH + (height * 0.04).toInt()
    val mascotSize = (width * 0.65).toInt()
    val mascotY = height - mascotSize - (height * 0.05).toInt()

    val filter = buildString {
        append("color=c='${style.backgroundColor}':s=${width}x${height}[bg];")
        append("[0:v]scale=$panelW:$panelH:force_original_aspect_ratio=decrease,pad=$panelW:$panelH:(ow-iw)/2:(oh-ih)/2:color=white@0.0[a];")
        append("[1:v]scale=$panelW:$panelH:force_original_aspect_ratio=decrease,pad=$panelW:$panelH:(ow-iw)/2:(oh-ih)/2:color=white@0.0[b];")
        append("[2:v]scale=$mascotSize:$mascotSize:force_original_aspect_ratio=decrease[m];")
        append("[bg][a]overlay=x=$panelAX:y=$panelY[s1];")
        append("[s1][b]overlay=x=$panelBX:y=$panelY[s2];")
        append("[s2][m]overlay=x=(W-w)/2:y=$mascotY[s3];")
        append("[s3]drawtext=text='${labelA.escapeDrawtext()}':font='${style.labelFontFamily}':fontsize=${style.labelFontSize}:fontcolor=${style.labelColor}:x=$panelAX+($panelW-text_w)/2:y=$labelY,")
        append("drawtext=text='${labelB.escapeDrawtext()}':font='${style.labelFontFamily}':fontsize=${style.labelFontSize}:fontcolor=${style.labelColor}:x=$panelBX+($panelW-text_w)/2:y=$labelY,")
        append("drawtext=text='${caption.escapeDrawtext()}':font='${style.captionFontFamily}':fontsize=${style.captionFontSize}:fontcolor=${style.captionColor}:x=(w-text_w)/2:y=$captionY[out]")
    }

    val output = tempFile("comparison-layout", "png")
    run(
        ffmpeg, "-v", "error", "-y",
        "-i", sourceA.absolutePath,
        "-i", sourceB.absolutePath,
        "-i", mascot.absolutePath,
        "-filter_complex", filter,
        "-map", "[out]",
        "-frames:v", "1",
        output.absolutePath,
    )
    return inspectImage(output.absolutePath)
}

/** Escapes drawtext's `text=` value. Apostrophes become a curly quote rather than fighting
 * ffmpeg's nested single-quote escaping — same pragmatic spirit as [String.escapeAssField]. */
private fun String.escapeDrawtext(): String =
    replace("\\", "\\\\")
        .replace(":", "\\:")
        .replace("'", "’")
        .replace("%", "\\%")

/**
 * `-loop 1` feeds the single still image as an infinite source; `zoompan`'s `d` (frame count)
 * bounds it to exactly [fps] * duration frames, and `-t` trims the encode to that same duration —
 * without `-t`, the loop-1 input would otherwise let ffmpeg run past the zoompan window.
 */
internal suspend fun FfmpegMediaCoreBackend.kenBurnsImpl(
    imagePath: String,
    durationMs: Double,
    fps: Double,
    zoomStart: Double,
    zoomEnd: Double,
    panX: String,
    panY: String,
    width: Int,
    height: Int,
): VideoMetadata {
    require(durationMs > 0.0) { "Ken Burns duration_ms must be positive." }
    require(fps > 0.0) { "Ken Burns fps must be positive." }
    require(width > 0 && height > 0) { "Ken Burns dimensions must be positive." }
    val source = requireFile(imagePath, "Image")
    val durationSec = durationMs / 1000.0
    val frameCount = (fps * durationSec).toInt().coerceAtLeast(1)

    val xExpr = when (panX) {
        "left_to_right" -> "(iw-iw/zoom)*(on/$frameCount)"
        "right_to_left" -> "(iw-iw/zoom)*(1-on/$frameCount)"
        else -> "iw/2-(iw/zoom/2)"
    }
    val yExpr = when (panY) {
        "top_to_bottom" -> "(ih-ih/zoom)*(on/$frameCount)"
        "bottom_to_top" -> "(ih-ih/zoom)*(1-on/$frameCount)"
        else -> "ih/2-(ih/zoom/2)"
    }
    val zoomExpr = "$zoomStart+(${zoomEnd - zoomStart})*on/$frameCount"

    val output = tempFile("ken-burns", "mp4")
    run(
        ffmpeg, "-v", "error", "-y",
        "-loop", "1", "-i", source.absolutePath,
        "-vf", "zoompan=z='$zoomExpr':x='$xExpr':y='$yExpr':d=$frameCount:s=${width}x${height}:fps=$fps,format=yuv420p",
        "-t", durationSec.toString(),
        "-c:v", "libx264",
        "-movflags", "+faststart",
        output.absolutePath,
    )
    return inspectVideo(output.absolutePath)
}
