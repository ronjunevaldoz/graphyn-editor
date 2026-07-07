package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.common.EnvironmentResolver

/**
 * Zero-config fallbacks so the media-AI templates run out of the box on a typical desktop:
 * macOS `say` for text-to-speech and `tesseract` for OCR. A configured `GRAPHYN_*_EXECUTABLE`
 * always wins; these only fill in when the env var is unset and the system tool is present.
 */
internal fun resolveTtsEngine(): TextToSpeechEngine = when {
    EnvironmentResolver.get("GRAPHYN_TTS_EXECUTABLE")
        ?.isNotBlank() == true -> createTextToSpeechEngine()

    CommandResolver.isAvailable("say") -> createSystemTextToSpeechEngine()
    else -> createTextToSpeechEngine()
}

/**
 * Explicit per-engine overrides for the "engine" field on media.text_to_speech nodes.
 * These are opt-in only — [resolveTtsEngine] (the "auto"/default path, including the `say`
 * fallback) is untouched by these.
 */
internal fun resolveQwen3TtsEngine(): TextToSpeechEngine = CommandTextToSpeechEngine(
    executable = EnvironmentResolver.get("GRAPHYN_TTS_QWEN3_EXECUTABLE")?.takeIf(String::isNotBlank),
    envVarName = "GRAPHYN_TTS_QWEN3_EXECUTABLE",
)

internal fun resolveOuteTtsEngine(): TextToSpeechEngine = CommandTextToSpeechEngine(
    executable = EnvironmentResolver.get("GRAPHYN_TTS_OUTE_EXECUTABLE")?.takeIf(String::isNotBlank),
    envVarName = "GRAPHYN_TTS_OUTE_EXECUTABLE",
)

internal fun resolveOcrEngine(): OcrEngine = when {
//    EnvironmentResolver.get("GRAPHYN_OCR_EXECUTABLE")?.isNotBlank() == true -> CommandOcrEngine()
    CommandResolver.isAvailable("tesseract") -> createOcrEngine()
    else -> error("Tesseract not yet installed")
}