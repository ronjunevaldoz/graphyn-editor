package com.ronjunevaldoz.graphyn.plugins.mediaai

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class OcrBlock(
    val text: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val confidence: Double,
)

data class OcrResult(
    val text: String,
    val blocks: List<OcrBlock>,
)

/** Extracts text from an image. Implementations are CLI adapters, mirroring [SpeechToTextEngine]. */
fun interface OcrEngine {
    suspend fun recognize(imagePath: String, language: String): OcrResult
}

/**
 * Runs `GRAPHYN_OCR_EXECUTABLE --image <path> --language <lang>` and parses JSON from stdout:
 * `{"text": "...", "blocks": [{"text": "...", "x": 0, "y": 0, "width": 0, "height": 0, "confidence": 0.0}]}`.
 */
class CommandOcrEngine(
    private val executable: String? = System.getenv("GRAPHYN_OCR_EXECUTABLE")?.takeIf(String::isNotBlank),
) : OcrEngine {
    override suspend fun recognize(imagePath: String, language: String): OcrResult = withContext(Dispatchers.IO) {
        val command = executable ?: error(
            "OCR requires GRAPHYN_OCR_EXECUTABLE. " +
                "Configure a CLI adapter as documented in plugins/media-ai/README.md.",
        )
        require(File(imagePath).isFile) { "OCR image file does not exist: $imagePath" }
        val process = try {
            ProcessBuilder(command, "--image", imagePath, "--language", language).start()
        } catch (error: Exception) {
            error("Unable to start OCR adapter '$command': ${error.message}")
        }
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "OCR adapter failed with exit code $exitCode: ${stderr.trim().ifEmpty { "unknown error" }}" }
        parseOcrJson(stdout)
    }
}

internal fun parseOcrJson(raw: String): OcrResult {
    val root = Json.parseToJsonElement(raw).jsonObject
    val blocks = root["blocks"]?.jsonArray.orEmpty().map { element ->
        val fields = element.jsonObject
        fun int(key: String) = fields[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        OcrBlock(
            text = fields["text"]?.jsonPrimitive?.content.orEmpty(),
            x = int("x"),
            y = int("y"),
            width = int("width"),
            height = int("height"),
            confidence = fields["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 1.0,
        )
    }
    return OcrResult(
        text = root["text"]?.jsonPrimitive?.content ?: error("OCR adapter returned no 'text'."),
        blocks = blocks,
    )
}
