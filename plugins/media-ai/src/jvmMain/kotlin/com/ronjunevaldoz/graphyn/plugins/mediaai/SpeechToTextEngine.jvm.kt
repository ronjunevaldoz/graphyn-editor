package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.common.EnvironmentResolver
import com.ronjunevaldoz.graphyn.core.common.FileIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual fun createSpeechToTextEngine(): SpeechToTextEngine =
    CommandSpeechToTextEngine()

class CommandSpeechToTextEngine(
    private val ffmpegExecutable: String =
        EnvironmentResolver.get("FFMPEG_EXECUTABLE")?.takeIf(String::isNotBlank) ?: "ffmpeg",
    private val modelPath: String =
        EnvironmentResolver.get("WHISPER_MODEL_PATH")?.takeIf(String::isNotBlank)
            ?: FileIO.resolvePath("~/.graphyn/models/whisper", "ggml-base.bin"),
) : SpeechToTextEngine {

    override suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult =
        withContext(Dispatchers.IO) {
            val audioFile = File(request.audioPath)

            require(audioFile.isFile) {
                "Speech to Text audio file does not exist: ${request.audioPath}"
            }

            val tempJsonFile = File.createTempFile("stt_output_", ".json")

            try {
                val process = ProcessBuilder(
                    ffmpegExecutable,
                    "-y",
                    "-i", audioFile.absolutePath,
                    "-vn",
                    "-af",
                    "whisper=model=$modelPath:language=${request.language}:destination=${tempJsonFile.absolutePath}:format=json",
                    "-f", "null",
                    "-",
                ).start()

                val stderr = process.errorStream.bufferedReader().readText()
                val exitCode = process.waitFor()

                check(exitCode == 0) {
                    "FFmpeg STT failed with exit code $exitCode: ${
                        stderr.trim().ifEmpty { "unknown error" }
                    }"
                }

                parseSttJson(tempJsonFile.readText())
            } catch (error: Exception) {
                error("Unable to run FFmpeg STT process: ${error.message}")
            } finally {
                tempJsonFile.delete()
            }
        }
}