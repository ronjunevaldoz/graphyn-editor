package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.ArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.FileArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdImageResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdVideoResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionBackend
import java.io.File

/**
 * Desktop wiring for the published [com.ronjunevaldoz.graphyn.plugins.stablesd.http.HttpStableDiffusionBackend]:
 * adds a recording of every generation to [history] (a desktop-only nicety — the plugin's backend
 * itself has no opinion on persistence beyond writing the output file).
 */
class HttpStableDiffusionBackend(
    settingsStore: SettingsStore = FileSettingsStore(),
    artifactsDir: File = defaultArtifactsDir(),
    private val history: ArtifactHistory = FileArtifactHistory(),
) : StableDiffusionBackend {
    private val delegate = com.ronjunevaldoz.graphyn.plugins.stablesd.http.HttpStableDiffusionBackend(settingsStore, artifactsDir)

    override suspend fun generateImage(request: SdGenerateImageRequest): SdImageResult {
        val start = System.currentTimeMillis()
        val result = delegate.generateImage(request)
        history.record(buildArtifactRecord(File(result.imagePaths.first()), ArtifactKind.Image, request, System.currentTimeMillis() - start, "sd.txt2img"))
        return result
    }

    override suspend fun generateVideo(request: SdGenerateVideoRequest): SdVideoResult {
        val start = System.currentTimeMillis()
        val result = delegate.generateVideo(request)
        history.record(buildArtifactRecord(File(result.framePaths.first()), ArtifactKind.Video, request, System.currentTimeMillis() - start, "sd.img2vid"))
        return result
    }

    private companion object {
        fun defaultArtifactsDir(): File =
            System.getenv("GRAPHYN_ARTIFACTS_DIR")?.ifBlank { null }?.let(::File)
                ?: File(System.getProperty("user.home"), ".graphyn/artifacts")
    }
}
