package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.common.EnvironmentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun createTextToSpeechEngine(): TextToSpeechEngine =
    CommandTextToSpeechEngine()

class CommandTextToSpeechEngine(
    private val executable: String? =
        EnvironmentResolver.get("GRAPHYN_TTS_EXECUTABLE")?.takeIf(String::isNotBlank),
    private val envVarName: String = "GRAPHYN_TTS_EXECUTABLE",
) : TextToSpeechEngine {

    override suspend fun synthesize(
        request: TextToSpeechRequest,
        outputPath: String,
    ) = withContext(Dispatchers.IO) {
        val command = executable ?: error(
            "Text to Speech requires $envVarName. " +
                    "Configure a CLI adapter as documented in plugins/media-ai/README.md.",
        )

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val args = mutableListOf(
            command,
            "--text", request.text,
            "--language", request.language,
            "--voice", request.voiceId,
            "--speed", request.speed.toString(),
            "--output", outputFile.absolutePath,
        )
        if (request.referenceAudioPath.isNotBlank()) args += listOf("--reference-audio", request.referenceAudioPath)
        if (request.instruct.isNotBlank()) args += listOf("--instruct", request.instruct)
        if (request.temperature >= 0.0) args += listOf("--temperature", request.temperature.toString())
        request.seed?.let { args += listOf("--seed", it.toString()) }

        val process = try {
            ProcessBuilder(args).redirectErrorStream(true).start()
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