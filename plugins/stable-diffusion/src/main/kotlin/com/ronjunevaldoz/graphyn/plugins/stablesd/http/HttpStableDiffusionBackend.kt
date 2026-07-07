package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdImageResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdVideoResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionBackend
import java.io.File

/**
 * [StableDiffusionBackend] that forwards generation to a running `server-sd` over HTTP: serializes
 * the typed request into `/api/sd/generate-ex` (image) or `/api/sd/generate-video` (video) via
 * [ServerSdClient], then writes the returned bytes to [outputDir]. Any recording of the result
 * (history, DB, etc.) is the caller's concern — this class only produces files and paths, matching
 * [StableDiffusionBackend]'s contract.
 */
class HttpStableDiffusionBackend(
    settingsStore: SettingsStore = FileSettingsStore(),
    private val outputDir: File = defaultOutputDir(),
) : StableDiffusionBackend {
    private val client = ServerSdClient(settingsStore)

    override suspend fun generateImage(request: SdGenerateImageRequest): SdImageResult {
        val file = saveOutput(client.generateImage(request), "png")
        return SdImageResult(imagePaths = listOf(file.absolutePath))
    }

    override suspend fun generateVideo(request: SdGenerateVideoRequest): SdVideoResult {
        val file = saveOutput(client.generateVideo(request), "mp4")
        return SdVideoResult(framePaths = listOf(file.absolutePath))
    }

    private fun saveOutput(bytes: ByteArray, ext: String): File {
        outputDir.mkdirs()
        val file = File(outputDir, "graphyn-sd-${System.currentTimeMillis()}-${counter.incrementAndGet()}.$ext")
        file.writeBytes(bytes)
        return file
    }

    private companion object {
        val counter = java.util.concurrent.atomic.AtomicLong(0)

        fun defaultOutputDir(): File =
            System.getenv("GRAPHYN_SD_OUTPUT_DIR")?.ifBlank { null }?.let(::File)
                ?: System.getenv("GRAPHYN_ARTIFACTS_DIR")?.ifBlank { null }?.let(::File)
                ?: File(System.getProperty("user.home"), ".graphyn/artifacts")
    }
}
