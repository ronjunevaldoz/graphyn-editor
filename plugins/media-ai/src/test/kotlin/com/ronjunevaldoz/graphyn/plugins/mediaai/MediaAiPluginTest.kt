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
    fun registersAllSpecsAndExecutors() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(MediaAiPlugin())

        assertEquals(4, registry.nodeSpecs.all().size)
        MediaAiSpecs.all.forEach {
            assertNotNull(registry.nodeSpecs.resolve(it.type))
            assertNotNull(registry.nodeExecutors.resolve(it.type))
        }
    }

    @Test
    fun speechToTextEmitsTextAndTimedSegments() = runTest {
        val engine = SpeechToTextEngine { request ->
            assertEquals("en", request.language)
            SpeechToTextResult(
                text = "hello world",
                confidence = 0.9,
                segments = listOf(
                    SpeechSegment("hello", 0.0, 500.0),
                    SpeechSegment("world", 500.0, 1000.0),
                ),
            )
        }
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(speechToTextEngine = engine))
        }
        val result = registry.nodeExecutors.resolve(MediaAiSpecs.speechToText.type)!!.execute(
            mapOf(
                "audio" to MediaTypes.audioValue("/media/voice.wav"),
                "language" to WorkflowValue.StringValue("en"),
            ),
        )
        assertEquals(WorkflowValue.StringValue("hello world"), result["text"])
        assertEquals(WorkflowValue.DoubleValue(0.9), result["confidence"])
        val segments = (result["segments"] as WorkflowValue.ListValue).items
        assertEquals(2, segments.size)
        val first = (segments.first() as WorkflowValue.RecordValue).fields
        assertEquals(WorkflowValue.StringValue("hello"), first["text"])
        assertEquals(WorkflowValue.DoubleValue(500.0), first["end_ms"])
    }

    @Test
    fun ocrEmitsTextAndBlocks() = runTest {
        val engine = OcrEngine { imagePath, _ ->
            assertEquals("/media/frame.png", imagePath)
            OcrResult(
                text = "INVOICE",
                blocks = listOf(OcrBlock("INVOICE", x = 5, y = 6, width = 80, height = 20, confidence = 0.95)),
            )
        }
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(ocrEngine = engine))
        }
        val result = registry.nodeExecutors.resolve(MediaAiSpecs.ocr.type)!!.execute(
            mapOf("image" to MediaTypes.imageValue("/media/frame.png")),
        )
        assertEquals(WorkflowValue.StringValue("INVOICE"), result["text"])
        val blocks = (result["blocks"] as WorkflowValue.ListValue).items
        val block = (blocks.single() as WorkflowValue.RecordValue).fields
        assertEquals(WorkflowValue.IntValue(80), block["width"])
        assertEquals(WorkflowValue.DoubleValue(0.95), block["confidence"])
    }

    @Test
    fun jsonAdaptersParseSegmentsAndBlocks() {
        val stt = parseSttJson(
            """{"text":"hi","confidence":0.8,"segments":[{"text":"hi","start_ms":0,"end_ms":250}]}""",
        )
        assertEquals("hi", stt.text)
        assertEquals(250.0, stt.segments.single().endMs)

        val ocr = parseOcrJson(
            """{"text":"A","blocks":[{"text":"A","x":1,"y":2,"width":3,"height":4,"confidence":0.5}]}""",
        )
        assertEquals(3, ocr.blocks.single().width)
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
