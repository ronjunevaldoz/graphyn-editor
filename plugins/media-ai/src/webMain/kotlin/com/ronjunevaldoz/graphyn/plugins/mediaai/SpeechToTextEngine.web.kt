package com.ronjunevaldoz.graphyn.plugins.mediaai

actual fun createSpeechToTextEngine(): SpeechToTextEngine =
    UnsupportedSpeechToTextEngine

private object UnsupportedSpeechToTextEngine : SpeechToTextEngine {
    override suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult {
        error("Speech to Text is not supported on this platform.")
    }
}