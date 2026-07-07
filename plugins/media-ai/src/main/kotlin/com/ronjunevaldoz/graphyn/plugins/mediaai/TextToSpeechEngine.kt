package com.ronjunevaldoz.graphyn.plugins.mediaai

data class TextToSpeechRequest(
    val text: String,
    val language: String,
    val voiceId: String,
    val speed: Double,
    // Distinguishes cache entries produced by different engines (say/qwen3/oute) so the
    // same text/voice/speed doesn't return another engine's cached audio (see TtsCacheEngineJvm).
    val engineId: String = "auto",
    // Engine-specific extras, blank/sentinel by default so the base --text/--language/--voice/
    // --speed/--output contract is unaffected for adapters that don't use them.
    val referenceAudioPath: String = "",
    val instruct: String = "",
    val temperature: Double = -1.0,
    val seed: Int? = null,
)

fun interface TextToSpeechEngine {
    suspend fun synthesize(
        request: TextToSpeechRequest,
        outputPath: String,
    )
}

