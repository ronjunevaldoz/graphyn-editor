package com.ronjunevaldoz.graphyn.plugins.mediacore

/** Phase 3 image operations for [FfmpegMediaCoreBackend]; see that class for the process plumbing. */
internal suspend fun FfmpegMediaCoreBackend.resizeImageImpl(imagePath: String, width: Int, height: Int): ImageMetadata {
    require(width > 0 && height > 0) { "Image Resize dimensions must be positive." }
    val source = requireFile(imagePath, "Image")
    val output = tempFile("image-resize", "png")
    run(ffmpeg, "-v", "error", "-y", "-i", source.absolutePath, "-vf", "scale=$width:$height", "-frames:v", "1", output.absolutePath)
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
    run(ffmpeg, "-v", "error", "-y", "-i", source.absolutePath, "-vf", "crop=$width:$height:$x:$y", "-frames:v", "1", output.absolutePath)
    return inspectImage(output.absolutePath)
}

internal suspend fun FfmpegMediaCoreBackend.imageSequenceToVideoImpl(imagePaths: List<String>, fps: Double): VideoMetadata {
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
