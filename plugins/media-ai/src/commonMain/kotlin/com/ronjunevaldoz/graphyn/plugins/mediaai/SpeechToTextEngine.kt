package com.ronjunevaldoz.graphyn.plugins.mediaai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
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
 * Transcribes audio to timed text.
 *
 * Platform implementations may use native CLI adapters, system APIs,
 * or return an unsupported implementation on platforms without filesystem/process access.
 */
interface SpeechToTextEngine {
    suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult
}

expect fun createSpeechToTextEngine(): SpeechToTextEngine

internal fun parseSttJson(raw: String): SpeechToTextResult {
    val lines = raw
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()

    val segments = lines.map { line ->
        val fields = Json.parseToJsonElement(line).jsonObject

        SpeechSegment(
            text = fields["text"]?.jsonPrimitive?.content.orEmpty(),
            startMs = (fields["start"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000.0,
            endMs = (fields["end"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000.0,
        )
    }

    return SpeechToTextResult(
        text = segments.joinToString(" ") { it.text }.trim(),
        confidence = 1.0,
        segments = segments,
    )
}