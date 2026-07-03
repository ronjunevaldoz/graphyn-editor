package com.ronjunevaldoz.graphyn.plugins.mediaai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

fun createSystemTextToSpeechEngine(): TextToSpeechEngine = SayTextToSpeechEngine()

private class SayTextToSpeechEngine(
    private val executable: String = "say",
) : TextToSpeechEngine {

    override suspend fun synthesize(
        request: TextToSpeechRequest,
        outputPath: String,
    ) = withContext(Dispatchers.IO) {

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val args = mutableListOf(
            executable,
            "--file-format=WAVE",
            "--data-format=LEI16@22050",
            "-o",
            outputFile.absolutePath,
        )

        if (request.voiceId.isNotBlank() && request.voiceId != "default") {
            args += listOf("-v", request.voiceId)
        }

        args += listOf(
            "-r",
            (175 * request.speed).roundToInt().toString(),
        )

        args += request.text

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()

        check(process.waitFor() == 0) {
            "say failed: ${output.trim().ifEmpty { "unknown error" }}"
        }

        check(outputFile.isFile && outputFile.length() > 0L) {
            "say produced no audio at ${outputFile.absolutePath}."
        }
    }
}