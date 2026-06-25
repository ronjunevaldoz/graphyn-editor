package com.ronjunevaldoz.graphyn.plugins.mediaai

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TextToSpeechRequest(
    val text: String,
    val language: String,
    val voiceId: String,
    val speed: Double,
)

fun interface TextToSpeechEngine {
    suspend fun synthesize(request: TextToSpeechRequest, outputFile: File)
}

class CommandTextToSpeechEngine(
    private val executable: String? = System.getenv("GRAPHYN_TTS_EXECUTABLE")?.takeIf(String::isNotBlank),
) : TextToSpeechEngine {
    override suspend fun synthesize(request: TextToSpeechRequest, outputFile: File) = withContext(Dispatchers.IO) {
        val command = executable ?: error(
            "Text to Speech requires GRAPHYN_TTS_EXECUTABLE. " +
                "Configure a CLI adapter as documented in plugins/media-ai/README.md.",
        )
        outputFile.parentFile?.mkdirs()
        val process = try {
            ProcessBuilder(
                command,
                "--text", request.text,
                "--language", request.language,
                "--voice", request.voiceId,
                "--speed", request.speed.toString(),
                "--output", outputFile.absolutePath,
            ).redirectErrorStream(true).start()
        } catch (error: Exception) {
            error("Unable to start TTS adapter '$command': ${error.message}")
        }
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "TTS adapter failed with exit code $exitCode: ${output.trim().ifEmpty { "unknown error" }}"
        }
        check(outputFile.isFile && outputFile.length() > 0L) {
            "TTS adapter completed without creating ${outputFile.absolutePath}."
        }
    }
}
