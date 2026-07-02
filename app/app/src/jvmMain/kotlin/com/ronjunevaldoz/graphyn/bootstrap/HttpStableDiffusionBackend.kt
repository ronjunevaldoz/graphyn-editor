package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.ArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.FileArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdImageResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdVideoResult
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionBackend
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Forwards generation to a running `server-sd` over HTTP: translates the executor's CLI-flag list
 * into `/api/sd/generate-ex` (image) or `/api/sd/generate-video` (video), persists the bytes to a
 * durable artifacts dir, and records each run in [history].
 *
 * The server URL + API key are resolved fresh **per run** from [settingsStore] (then env, then
 * default), so a change in the credentials panel applies on the next generation without a restart.
 */
class HttpStableDiffusionBackend(
    private val settingsStore: SettingsStore = FileSettingsStore(),
    private val artifactsDir: File = defaultArtifactsDir(),
    private val history: ArtifactHistory = FileArtifactHistory(),
) : StableDiffusionBackend {
    private val api = ServerSdApi(settingsStore)

    override fun generateImage(args: List<String>): SdImageResult {
        val file = runBlocking {
            val start = System.currentTimeMillis()
            val bytes = api.generateImage(args)
            saveArtifact(bytes, "png").also {
                history.record(buildArtifactRecord(it, ArtifactKind.Image, args, System.currentTimeMillis() - start, "sd.txt2img"))
            }
        }
        return SdImageResult(imagePaths = listOf(file.absolutePath))
    }

    override fun generateVideo(args: List<String>): SdVideoResult {
        val file = runBlocking {
            val start = System.currentTimeMillis()
            val bytes = api.generateVideo(args)
            saveArtifact(bytes, "mp4").also {
                history.record(buildArtifactRecord(it, ArtifactKind.Video, args, System.currentTimeMillis() - start, "sd.img2vid"))
            }
        }
        return SdVideoResult(framePaths = listOf(file.absolutePath))
    }

    private fun saveArtifact(bytes: ByteArray, ext: String): File {
        artifactsDir.mkdirs()
        val file = File(artifactsDir, "graphyn-sd-${System.currentTimeMillis()}-${counter.incrementAndGet()}.$ext")
        file.writeBytes(bytes)
        return file
    }

    private companion object {
        val counter = java.util.concurrent.atomic.AtomicLong(0)

        fun defaultArtifactsDir(): File =
            System.getenv("GRAPHYN_ARTIFACTS_DIR")?.ifBlank { null }?.let(::File)
                ?: File(System.getProperty("user.home"), ".graphyn/artifacts")
    }
}
