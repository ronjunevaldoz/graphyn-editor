package com.ronjunevaldoz.graphyn.plugins.mediaai

data class TextToSpeechRequest(
    val text: String,
    val language: String,
    val voiceId: String,
    val speed: Double,
)

fun interface TextToSpeechEngine {
    suspend fun synthesize(
        request: TextToSpeechRequest,
        outputPath: String,
    )
}

