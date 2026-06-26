package com.ronjunevaldoz.graphyn.plugins.mediaai

import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Zero-config fallbacks so the media-AI templates run out of the box on a typical desktop:
 * macOS `say` for text-to-speech and `tesseract` for OCR. A configured `GRAPHYN_*_EXECUTABLE`
 * always wins; these only fill in when the env var is unset and the system tool is present.
 */
internal fun resolveTtsEngine(): TextToSpeechEngine = when {
    System.getenv("GRAPHYN_TTS_EXECUTABLE")?.isNotBlank() == true -> CommandTextToSpeechEngine()
    isCommandAvailable("say") -> SayTextToSpeechEngine()
    else -> CommandTextToSpeechEngine()
}

internal fun resolveOcrEngine(): OcrEngine = when {
    System.getenv("GRAPHYN_OCR_EXECUTABLE")?.isNotBlank() == true -> CommandOcrEngine()
    isCommandAvailable("tesseract") -> TesseractOcrEngine()
    else -> CommandOcrEngine()
}

internal fun isCommandAvailable(command: String): Boolean = try {
    ProcessBuilder("/usr/bin/which", command).redirectErrorStream(true).start().waitFor() == 0
} catch (_: Exception) {
    false
}

/** macOS `say`-backed TTS. Writes a WAV so it drops into the existing TTS cache unchanged. */
class SayTextToSpeechEngine(private val say: String = "say") : TextToSpeechEngine {
    override suspend fun synthesize(request: TextToSpeechRequest, outputFile: File) = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val args = mutableListOf(say, "--file-format=WAVE", "--data-format=LEI16@22050", "-o", outputFile.absolutePath)
        if (request.voiceId.isNotBlank() && request.voiceId != "default") args += listOf("-v", request.voiceId)
        args += listOf("-r", (175 * request.speed).roundToInt().toString())
        args += request.text
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "say failed: ${output.trim().ifEmpty { "unknown error" }}" }
        check(outputFile.isFile && outputFile.length() > 0L) { "say produced no audio at ${outputFile.absolutePath}." }
    }
}

/** `tesseract`-backed OCR. Returns plain text; bounding blocks are left empty in the fallback. */
class TesseractOcrEngine(private val tesseract: String = "tesseract") : OcrEngine {
    override suspend fun recognize(imagePath: String, language: String): OcrResult = withContext(Dispatchers.IO) {
        require(File(imagePath).isFile) { "OCR image file does not exist: $imagePath" }
        val process = ProcessBuilder(tesseract, imagePath, "stdout", "-l", tesseractLanguage(language)).start()
        val text = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "tesseract failed: ${error.trim().ifEmpty { "unknown error" }}" }
        OcrResult(text = text.trim(), blocks = emptyList())
    }
}

private fun tesseractLanguage(language: String): String = when (language) {
    "en" -> "eng"
    "zh" -> "chi_sim"
    "es" -> "spa"
    "fr" -> "fra"
    "de" -> "deu"
    "ja" -> "jpn"
    "ko" -> "kor"
    else -> "eng"
}
