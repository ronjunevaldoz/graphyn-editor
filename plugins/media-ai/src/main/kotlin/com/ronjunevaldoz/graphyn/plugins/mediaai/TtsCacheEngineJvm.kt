package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.plugins.mediacore.FfmpegMediaCoreBackend
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

fun createTtCacheEngine(): TtsCacheEngine = JvmTtsCache()
class JvmTtsCache(
    private val directory: File = defaultCacheDirectory(),
    private val metadataReader: AudioMetadataReader = AudioMetadataReader {
        FfmpegMediaCoreBackend().inspectAudio(it)
    },
) : TtsCacheEngine {
    private val writeMutex = Mutex()

    override suspend fun getOrCreate(
        request: TextToSpeechRequest,
        engine: TextToSpeechEngine,
    ): CachedSpeech = writeMutex.withLock {
        require(request.text.isNotBlank()) { "Text to Speech requires non-blank text." }
        require(request.speed in 0.5..2.0) { "Text to Speech speed must be between 0.5 and 2.0." }

        directory.mkdirs()

        val key = request.cacheKey()
        val output = File(directory, "$key.wav")

        if (output.isFile && output.length() > 0L) {
            return CachedSpeech(
                metadata = metadataReader.read(output.absolutePath),
                cacheHit = true,
            )
        }

        val partial = File(directory, "$key.partial.wav")
        partial.delete()

        try {
            engine.synthesize(request, partial.absolutePath)

            check(partial.isFile && partial.length() > 0L) {
                "TTS engine produced no audio."
            }

            if (!partial.renameTo(output)) {
                partial.copyTo(output, overwrite = true)
                partial.delete()
            }
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }

        CachedSpeech(
            metadata = metadataReader.read(output.absolutePath),
            cacheHit = false,
        )
    }
}

private fun defaultCacheDirectory(): File {
    val configuredHome = System.getenv("GRAPHYN_HOME")?.takeIf(String::isNotBlank)
    val graphynHome = File(configuredHome ?: File(System.getProperty("user.home"), ".graphyn").path)
    return File(graphynHome, "cache/tts")
}

private fun TextToSpeechRequest.cacheKey(): String {
    val input = listOf(
        text, language, voiceId, speed.toString(), engineId,
        referenceAudioPath, instruct, temperature.toString(), seed.toString(),
    ).joinToString(separator = "\u0000")
    return MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
