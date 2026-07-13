package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.workflows.*
import com.ronjunevaldoz.graphyn.core.model.doubleValue
import com.ronjunevaldoz.graphyn.core.model.intValue
import com.ronjunevaldoz.graphyn.core.model.stringValue
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkflowCliOptionsTest {
    @Test
    fun resolvesQwen3CustomVoiceSettings() {
        val choice = resolveTtsEngineChoice(
            mapOf(
                "tts_engine" to "qwen3",
                "voice" to "Ryan",
                "reference_audio_path" to "/tmp/voice.wav",
                "temperature" to "0.2",
            ),
            TtsEngineChoice("qwen3", mapOf("voice" to stringValue("Ryan"))),
        )

        assertEquals("qwen3", choice.engine)
        assertEquals(stringValue("Ryan"), choice.params["voice"])
        assertEquals(stringValue("/tmp/voice.wav"), choice.params["reference_audio_path"])
        assertEquals(doubleValue(0.2), choice.params["temperature"])
    }

    @Test
    fun resolvesOuteSettings() {
        val choice = resolveTtsEngineChoice(
            mapOf(
                "tts_engine" to "oute",
                "language" to "es",
                "voice" to "narrator",
                "instruct" to "calm and steady",
                "temperature" to "0.6",
                "seed" to "7",
            ),
            TtsEngineChoice("oute", mapOf("voice" to stringValue("default"))),
        )

        assertEquals("oute", choice.engine)
        assertEquals(stringValue("es"), choice.params["language"])
        assertEquals(stringValue("narrator"), choice.params["voice"])
        assertEquals(stringValue("calm and steady"), choice.params["instruct"])
        assertEquals(doubleValue(0.6), choice.params["temperature"])
        assertEquals(intValue(7), choice.params["seed"])
    }
}
