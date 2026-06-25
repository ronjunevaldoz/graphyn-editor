package com.ronjunevaldoz.graphyn.plugins.mediaai

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SpeechSegment(
    val text: String,
    val startMs: Double,
    val endMs: Double,
)

data class SpeechToTextRequest(
    val audioPath: String,
    val language: String,
)

data class SpeechToTextResult(
    val text: String,
    val confidence: Double,
    val segments: List<SpeechSegment>,
)

/**
 * Transcribes audio to timed text. Implementations are CLI adapters so the heavy ASR model stays
 * out of the JVM process, mirroring [TextToSpeechEngine].
 */
fun interface SpeechToTextEngine {
    suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult
}

/**
 * Runs `GRAPHYN_STT_EXECUTABLE --audio <path> --language <lang>` and parses JSON from stdout:
 * `{"text": "...", "confidence": 0.0, "segments": [{"text": "...", "start_ms": 0, "end_ms": 0}]}`.
 */
class CommandSpeechToTextEngine(
    private val executable: String? = System.getenv("GRAPHYN_STT_EXECUTABLE")?.takeIf(String::isNotBlank),
) : SpeechToTextEngine {
    override suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = withContext(Dispatchers.IO) {
        val command = executable ?: error(
            "Speech to Text requires GRAPHYN_STT_EXECUTABLE. " +
                "Configure a CLI adapter as documented in plugins/media-ai/README.md.",
        )
        require(File(request.audioPath).isFile) { "Speech to Text audio file does not exist: ${request.audioPath}" }
        val process = try {
            ProcessBuilder(command, "--audio", request.audioPath, "--language", request.language).start()
        } catch (error: Exception) {
            error("Unable to start STT adapter '$command': ${error.message}")
        }
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "STT adapter failed with exit code $exitCode: ${stderr.trim().ifEmpty { "unknown error" }}" }
        parseSttJson(stdout)
    }
}

internal fun parseSttJson(raw: String): SpeechToTextResult {
    val root = Json.parseToJsonElement(raw).jsonObject
    val segments = root["segments"]?.jsonArray.orEmpty().map { element ->
        val fields = element.jsonObject
        SpeechSegment(
            text = fields["text"]?.jsonPrimitive?.content.orEmpty(),
            startMs = fields["start_ms"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            endMs = fields["end_ms"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
        )
    }
    return SpeechToTextResult(
        text = root["text"]?.jsonPrimitive?.content ?: error("STT adapter returned no 'text'."),
        confidence = root["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 1.0,
        segments = segments,
    )
}
