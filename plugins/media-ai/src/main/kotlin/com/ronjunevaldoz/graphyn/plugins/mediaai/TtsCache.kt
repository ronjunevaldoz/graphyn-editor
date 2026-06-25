package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.plugins.mediacore.AudioMetadata
import com.ronjunevaldoz.graphyn.plugins.mediacore.FfmpegMediaCoreBackend
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface AudioMetadataReader {
    suspend fun read(path: String): AudioMetadata
}

data class CachedSpeech(
    val metadata: AudioMetadata,
    val cacheHit: Boolean,
)

class TtsCache(
    private val directory: File = defaultCacheDirectory(),
    private val metadataReader: AudioMetadataReader = AudioMetadataReader {
        FfmpegMediaCoreBackend().inspectAudio(it)
    },
) {
    private val writeMutex = Mutex()

    suspend fun getOrCreate(
        request: TextToSpeechRequest,
        engine: TextToSpeechEngine,
    ): CachedSpeech = writeMutex.withLock {
        require(request.text.isNotBlank()) { "Text to Speech requires non-blank text." }
        require(request.speed in 0.5..2.0) { "Text to Speech speed must be between 0.5 and 2.0." }
        directory.mkdirs()
        val output = File(directory, "${request.cacheKey()}.wav")
        if (output.isFile && output.length() > 0L) {
            return CachedSpeech(metadataReader.read(output.absolutePath), cacheHit = true)
        }

        val partial = File(directory, "${request.cacheKey()}.partial.wav")
        partial.delete()
        try {
            engine.synthesize(request, partial)
            check(partial.isFile && partial.length() > 0L) { "TTS engine produced no audio." }
            if (!partial.renameTo(output)) {
                partial.copyTo(output, overwrite = true)
                partial.delete()
            }
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
        return CachedSpeech(metadataReader.read(output.absolutePath), cacheHit = false)
    }
}

private fun defaultCacheDirectory(): File {
    val configuredHome = System.getenv("GRAPHYN_HOME")?.takeIf(String::isNotBlank)
    val graphynHome = File(configuredHome ?: File(System.getProperty("user.home"), ".graphyn").path)
    return File(graphynHome, "cache/tts")
}

private fun TextToSpeechRequest.cacheKey(): String {
    val input = listOf(text, language, voiceId, speed.toString()).joinToString(separator = "\u0000")
    return MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
