package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.mediacore.AudioMetadata
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaPromptEnhanceTest {
    @Test
    fun promptEnhanceBuildsAStructuredPrompt() = runBlocking {
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(
                textToSpeechEngine = fakeTts(),
                ttsCacheEngine = fakeCache(),
                speechToTextEngine = fakeStt(),
                ocrEngine = fakeOcr(),
            ))
        }
        val result = registry.nodeExecutors.resolve(promptEnhanceSpec.type)!!.execute(
            mapOf(
                "prompt" to WorkflowValue.StringValue("a neon alley at night"),
                "topic" to WorkflowValue.StringValue("street chase"),
                "visual_style" to WorkflowValue.StringValue("moody neon noir"),
                "camera_move" to WorkflowValue.StringValue("gentle push-in"),
                "lighting" to WorkflowValue.StringValue("cinematic lighting"),
                "details" to WorkflowValue.StringValue("rain slick pavement, reflections, shallow depth of field"),
            ),
        )
        assertTrue((result["prompt"] as WorkflowValue.StringValue).value.contains("a neon alley at night"))
        assertTrue((result["prompt"] as WorkflowValue.StringValue).value.contains("cinematic lighting"))
        assertEquals("blurry, low quality, cropped, watermark", (result["negative_prompt"] as WorkflowValue.StringValue).value)
    }

    @Test
    fun promptEnhanceFallsBackWhenPromptIsBlank() = runBlocking {
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(
                textToSpeechEngine = fakeTts(),
                ttsCacheEngine = fakeCache(),
                speechToTextEngine = fakeStt(),
                ocrEngine = fakeOcr(),
            ))
        }
        val result = registry.nodeExecutors.resolve(promptEnhanceSpec.type)!!.execute(
            mapOf(
                "prompt" to WorkflowValue.StringValue(""),
                "topic" to WorkflowValue.StringValue("street chase"),
                "visual_style" to WorkflowValue.StringValue("moody neon noir"),
            ),
        )
        assertTrue((result["prompt"] as WorkflowValue.StringValue).value.contains("street chase"))
    }
}

private fun fakeTts() = TextToSpeechEngine { _, _ -> }
private fun fakeCache() = object : TtsCacheEngine {
    override suspend fun getOrCreate(request: TextToSpeechRequest, engine: TextToSpeechEngine): CachedSpeech =
        CachedSpeech(AudioMetadata("/tmp/${request.voiceId}.wav", 22_050, 1.0), false)
}

private fun fakeStt() = object : SpeechToTextEngine {
    override suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult =
        SpeechToTextResult("", 1.0, emptyList())
}

private fun fakeOcr() = object : OcrEngine {
    override suspend fun recognize(imagePath: String, language: String): OcrResult = OcrResult("", emptyList())
}
