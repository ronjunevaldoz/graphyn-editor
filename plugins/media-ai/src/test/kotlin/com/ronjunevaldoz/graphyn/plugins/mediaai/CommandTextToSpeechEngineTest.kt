package com.ronjunevaldoz.graphyn.plugins.mediaai

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies CommandTextToSpeechEngine only appends --reference-audio/--instruct/--temperature/
 * --seed when the request actually sets them — a wrong sentinel here silently either omits a
 * real value or sends a garbage flag (e.g. `--temperature -1.0`) that could make a real adapter
 * exit non-zero. Uses a real recording shell script rather than mocking ProcessBuilder, so this
 * exercises the actual argument-building + process-launch path.
 */
class CommandTextToSpeechEngineTest {
    private fun makeRequest(
        referenceAudioPath: String = "",
        instruct: String = "",
        temperature: Double = -1.0,
        seed: Int? = null,
    ) = TextToSpeechRequest(
        text = "hello",
        language = "en",
        voiceId = "default",
        speed = 1.0,
        referenceAudioPath = referenceAudioPath,
        instruct = instruct,
        temperature = temperature,
        seed = seed,
    )

    private fun recordingScript(): File {
        val script = Files.createTempFile("tts-recording-adapter", ".sh").toFile()
        script.writeText(
            """
            #!/bin/sh
            echo "${'$'}@" > "${script.absolutePath}.args"
            prev=""
            for arg in "${'$'}@"; do
                if [ "${'$'}prev" = "--output" ]; then
                    printf '\1' > "${'$'}arg"
                fi
                prev="${'$'}arg"
            done
            exit 0
            """.trimIndent(),
        )
        script.setExecutable(true)
        return script
    }

    private suspend fun runAndCaptureArgs(request: TextToSpeechRequest): String {
        val script = recordingScript()
        val output = Files.createTempFile("tts-output", ".wav").toFile()
        output.delete()
        val engine = CommandTextToSpeechEngine(executable = script.absolutePath)
        engine.synthesize(request, output.absolutePath)
        return File("${script.absolutePath}.args").readText()
    }

    @Test
    fun omitsAllOptionalFlagsWhenUnset() = runTest {
        val args = runAndCaptureArgs(makeRequest())
        assertFalse(args.contains("--reference-audio"))
        assertFalse(args.contains("--instruct"))
        assertFalse(args.contains("--temperature"))
        assertFalse(args.contains("--seed"))
    }

    @Test
    fun includesReferenceAudioWhenSet() = runTest {
        val args = runAndCaptureArgs(makeRequest(referenceAudioPath = "/tmp/voice.wav"))
        assertTrue(args.contains("--reference-audio /tmp/voice.wav"))
    }

    @Test
    fun includesInstructWhenSet() = runTest {
        val args = runAndCaptureArgs(makeRequest(instruct = "speak slowly"))
        assertTrue(args.contains("--instruct speak slowly"))
    }

    @Test
    fun includesTemperatureOnlyWhenNonNegative() = runTest {
        assertFalse(runAndCaptureArgs(makeRequest(temperature = -1.0)).contains("--temperature"))
        assertTrue(runAndCaptureArgs(makeRequest(temperature = 0.7)).contains("--temperature 0.7"))
    }

    @Test
    fun includesSeedOnlyWhenSet() = runTest {
        assertFalse(runAndCaptureArgs(makeRequest(seed = null)).contains("--seed"))
        assertTrue(runAndCaptureArgs(makeRequest(seed = 42)).contains("--seed 42"))
    }
}
