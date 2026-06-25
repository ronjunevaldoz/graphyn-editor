package com.ronjunevaldoz.graphyn.plugins.mediacore

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FfmpegMediaCoreBackendTest {
    @Test
    fun processesGeneratedVideoWhenFfmpegIsAvailable() = runTest {
        val directory = Files.createTempDirectory("graphyn-media-test").toFile()
        val backend = FfmpegMediaCoreBackend(tempDirectory = directory)
        if (!backend.isAvailable()) return@runTest

        val source = File(directory, "source.mp4")
        val generated = ProcessBuilder(
            "ffmpeg", "-v", "error", "-y",
            "-f", "lavfi", "-i", "color=c=blue:s=160x90:d=0.5:r=10",
            "-f", "lavfi", "-i", "sine=frequency=440:duration=0.5",
            "-shortest",
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            source.absolutePath,
        ).start().waitFor()
        if (generated != 0) return@runTest

        val metadata = backend.inspectVideo(source.absolutePath)
        assertEquals(160, metadata.width)
        assertEquals(90, metadata.height)
        assertTrue(metadata.durationMs > 0)

        val extracted = backend.extractAudio(source.absolutePath)
        assertTrue(File(extracted.path).isFile)
        assertTrue(extracted.sampleRate > 0)

        val mixed = backend.mixAudio(listOf(extracted.path, extracted.path), listOf(1.0, 0.5))
        assertTrue(File(mixed.path).isFile)

        val stitched = backend.stitchVideos(listOf(source.absolutePath, source.absolutePath))
        assertTrue(File(stitched.path).isFile)
        assertTrue(stitched.durationMs >= metadata.durationMs)

        val encodedFile = File(directory, "encoded.mp4")
        val encoded = backend.encodeVideo(
            videoPath = stitched.path,
            audioPath = mixed.path,
            outputPath = encodedFile.absolutePath,
            bitrate = "low",
            codec = "h264",
        )
        assertTrue(File(encoded.path).isFile)
        assertTrue(encoded.sizeBytes > 0)
    }
}
