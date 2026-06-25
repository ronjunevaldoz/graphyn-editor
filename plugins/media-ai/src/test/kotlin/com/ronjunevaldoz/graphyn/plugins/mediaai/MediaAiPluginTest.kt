@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.mediacore.AudioMetadata
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MediaAiPluginTest {
    @Test
    fun registersBothSpecsAndExecutors() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(MediaAiPlugin())

        assertEquals(2, registry.nodeSpecs.all().size)
        MediaAiSpecs.all.forEach {
            assertNotNull(registry.nodeSpecs.resolve(it.type))
            assertNotNull(registry.nodeExecutors.resolve(it.type))
        }
    }

    @Test
    fun textToSpeechCachesByAllVoiceInputs() = runTest {
        val directory = Files.createTempDirectory("graphyn-tts-test").toFile()
        var synthesisCount = 0
        val engine = TextToSpeechEngine { _, output ->
            synthesisCount++
            output.writeBytes(byteArrayOf(1, 2, 3))
        }
        val cache = TtsCache(
            directory = directory,
            metadataReader = AudioMetadataReader { AudioMetadata(it, sampleRate = 24_000, durationMs = 750.0) },
        )
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(engine, cache))
        }
        val executor = registry.nodeExecutors.resolve(MediaAiSpecs.textToSpeech.type)!!
        val inputs = mapOf(
            "text" to WorkflowValue.StringValue("Hello"),
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("narrator"),
            "speed" to WorkflowValue.DoubleValue(1.25),
        )

        val first = executor.execute(inputs)
        val second = executor.execute(inputs)

        assertEquals(1, synthesisCount)
        assertEquals(WorkflowValue.BooleanValue(false), first["cached"])
        assertEquals(WorkflowValue.BooleanValue(true), second["cached"])
        assertEquals(WorkflowValue.DoubleValue(750.0), second["duration_ms"])
        MediaTypes.path(second["audio"], "audio")

        executor.execute(inputs + ("speed" to WorkflowValue.DoubleValue(1.0)))
        assertEquals(2, synthesisCount)
    }

    @Test
    fun captionStyleProducesTypedRecord() = runTest {
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaAiPlugin()) }
        val executor = registry.nodeExecutors.resolve(MediaAiSpecs.captionStyle.type)!!
        val result = executor.execute(
            mapOf(
                "color" to WorkflowValue.StringValue("#FFFFFF"),
                "background_color" to WorkflowValue.StringValue("#AA000000"),
                "font_size" to WorkflowValue.IntValue(32),
                "position" to WorkflowValue.StringValue("bottom"),
            ),
        )
        val style = result["style_config"] as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.IntValue(32), style.fields["font_size"])
    }
}
