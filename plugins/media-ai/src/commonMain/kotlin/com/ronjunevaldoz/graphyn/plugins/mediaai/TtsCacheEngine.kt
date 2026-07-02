package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.plugins.mediacore.AudioMetadata

fun interface AudioMetadataReader {
    suspend fun read(path: String): AudioMetadata
}

data class CachedSpeech(
    val metadata: AudioMetadata,
    val cacheHit: Boolean,
)
interface TtsCacheEngine {
    suspend fun getOrCreate(
        request: TextToSpeechRequest,
        engine: TextToSpeechEngine,
    ): CachedSpeech
}

expect fun createTtCacheEngine() : TtsCacheEngine