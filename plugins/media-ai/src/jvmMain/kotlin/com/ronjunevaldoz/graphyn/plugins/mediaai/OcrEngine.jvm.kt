package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.common.EnvironmentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual fun createOcrEngine(): OcrEngine =
    TesseractOcrEngine()

class TesseractOcrEngine(
    private val tesseract: String =
        EnvironmentResolver.get("GRAPHYN_TESSERACT_EXECUTABLE")?.takeIf(String::isNotBlank)
            ?: "tesseract",
) : OcrEngine {

    override suspend fun recognize(
        imagePath: String,
        language: String,
    ): OcrResult = withContext(Dispatchers.IO) {
        val imageFile = File(imagePath)

        require(imageFile.isFile) {
            "OCR image file does not exist: $imagePath"
        }

        val process = ProcessBuilder(
            tesseract,
            imageFile.absolutePath,
            "stdout",
            "-l",
            tesseractLanguage(language),
        ).start()

        val text = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        check(process.waitFor() == 0) {
            "tesseract failed: ${error.trim().ifEmpty { "unknown error" }}"
        }

        OcrResult(
            text = text.trim(),
            blocks = emptyList(),
        )
    }
}