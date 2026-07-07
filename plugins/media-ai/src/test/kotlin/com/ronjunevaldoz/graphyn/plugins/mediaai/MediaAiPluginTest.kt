package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.mediacore.AudioMetadata
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaAiPluginTest {
    @Test
    fun registersAllSpecsAndExecutors() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(MediaAiPlugin())

        assertEquals(8, registry.nodeSpecs.all().size)
        MediaAiSpecs.all.forEach {
            assertNotNull(registry.nodeSpecs.resolve(it.type))
            assertNotNull(registry.nodeExecutors.resolve(it.type))
        }
        assertNotNull(registry.nodeSpecs.resolve(promptEnhanceSpec.type))
        assertNotNull(registry.nodeExecutors.resolve(promptEnhanceSpec.type))
    }

    @Test
    fun speechToTextEmitsTextAndTimedSegments(): Unit = runTest {
        val engine = object : SpeechToTextEngine {
            override suspend fun transcribe(request: SpeechToTextRequest): SpeechToTextResult {
                assertEquals("en", request.language)
                return SpeechToTextResult(
                    text = "hello world",
                    confidence = 0.9,
                    segments = listOf(
                        SpeechSegment("hello", 0.0, 500.0),
                        SpeechSegment("world", 500.0, 1000.0),
                    ),
                )
            }
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
        val engine = OcrEngine { imagePath: String, _: String ->
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
    fun fallbackResolversAlwaysReturnAnEngine() {
        assertNotNull(resolveTtsEngine())
    }

    @Test
    fun jsonAdaptersParseSegments() {
        val stt = parseSttJson(
            """{"text":"hi","start":0,"end":250}""",
        )
        assertEquals("hi", stt.text)
        assertEquals(250.0, stt.segments.single().endMs)
    }

    @Test
    fun textToSpeechCachesByAllVoiceInputs() = runTest {
        val directory = Files.createTempDirectory("graphyn-tts-test").toFile()
        var synthesisCount = 0
        val engine = TextToSpeechEngine { _: TextToSpeechRequest, output: String ->
            synthesisCount++
            File(output).writeBytes(byteArrayOf(1, 2, 3))
        }
        val cache = JvmTtsCache(
            directory = directory,
            metadataReader = AudioMetadataReader {
                AudioMetadata(it, sampleRate = 24_000, durationMs = 750.0)
            },
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
    fun differentEnginesDoNotShareCache() = runTest {
        // Regression guard for the cache key: same text/voice on the qwen3 vs oute dedicated
        // nodes must synthesize twice, not have the second serve the first's cached wav — see
        // TtsCacheEngineJvm's cacheKey(), which includes engineId for exactly this reason.
        val directory = Files.createTempDirectory("graphyn-tts-engine-cache-test").toFile()
        var qwen3Count = 0
        var outeCount = 0
        val qwen3Engine = TextToSpeechEngine { _, output -> qwen3Count++; File(output).writeBytes(byteArrayOf(1)) }
        val outeEngine = TextToSpeechEngine { _, output -> outeCount++; File(output).writeBytes(byteArrayOf(2)) }
        val cache = JvmTtsCache(
            directory = directory,
            metadataReader = AudioMetadataReader { AudioMetadata(it, sampleRate = 24_000, durationMs = 500.0) },
        )
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(ttsCacheEngine = cache, qwen3TextToSpeechEngine = qwen3Engine, outeTextToSpeechEngine = outeEngine))
        }
        val qwen3Executor = registry.nodeExecutors.resolve(MediaAiSpecs.textToSpeechQwen3.type)!!
        val outeExecutor = registry.nodeExecutors.resolve(MediaAiSpecs.textToSpeechOute.type)!!

        val sharedInputs = mapOf(
            "text" to WorkflowValue.StringValue("Hello"),
            "voice" to WorkflowValue.StringValue("shared"),
        )
        qwen3Executor.execute(sharedInputs)
        outeExecutor.execute(sharedInputs)

        assertEquals(1, qwen3Count)
        assertEquals(1, outeCount)
    }

    @Test
    fun sameEngineDifferentTemperatureDoesNotShareCache() = runTest {
        val directory = Files.createTempDirectory("graphyn-tts-temperature-cache-test").toFile()
        var synthesisCount = 0
        val engine = TextToSpeechEngine { _, output -> synthesisCount++; File(output).writeBytes(byteArrayOf(1)) }
        val cache = JvmTtsCache(
            directory = directory,
            metadataReader = AudioMetadataReader { AudioMetadata(it, sampleRate = 24_000, durationMs = 500.0) },
        )
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(ttsCacheEngine = cache, qwen3TextToSpeechEngine = engine))
        }
        val executor = registry.nodeExecutors.resolve(MediaAiSpecs.textToSpeechQwen3.type)!!

        executor.execute(mapOf("text" to WorkflowValue.StringValue("Hello"), "temperature" to WorkflowValue.DoubleValue(0.1)))
        executor.execute(mapOf("text" to WorkflowValue.StringValue("Hello"), "temperature" to WorkflowValue.DoubleValue(0.9)))

        assertEquals(2, synthesisCount)
    }

    @Test
    fun qwen3ExecutorMapsInputsToRequestFields() = runTest {
        var captured: TextToSpeechRequest? = null
        val engine = TextToSpeechEngine { request, output -> captured = request; File(output).writeBytes(byteArrayOf(1)) }
        val cache = JvmTtsCache(
            directory = Files.createTempDirectory("graphyn-tts-qwen3-plumbing-test").toFile(),
            metadataReader = AudioMetadataReader { AudioMetadata(it, sampleRate = 24_000, durationMs = 500.0) },
        )
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(ttsCacheEngine = cache, qwen3TextToSpeechEngine = engine))
        }
        val executor = registry.nodeExecutors.resolve(MediaAiSpecs.textToSpeechQwen3.type)!!
        executor.execute(
            mapOf(
                "text" to WorkflowValue.StringValue("Hello"),
                "voice" to WorkflowValue.StringValue("Ryan"),
                "reference_audio_path" to WorkflowValue.StringValue("/tmp/ref.wav"),
                "temperature" to WorkflowValue.DoubleValue(0.3),
            ),
        )
        val request = captured!!
        assertEquals("Hello", request.text)
        assertEquals("Ryan", request.voiceId)
        assertEquals("/tmp/ref.wav", request.referenceAudioPath)
        assertEquals(0.3, request.temperature)
        assertEquals("qwen3", request.engineId)
    }

    @Test
    fun outeExecutorMapsInputsToRequestFields() = runTest {
        var captured: TextToSpeechRequest? = null
        val engine = TextToSpeechEngine { request, output -> captured = request; File(output).writeBytes(byteArrayOf(1)) }
        val cache = JvmTtsCache(
            directory = Files.createTempDirectory("graphyn-tts-oute-plumbing-test").toFile(),
            metadataReader = AudioMetadataReader { AudioMetadata(it, sampleRate = 24_000, durationMs = 500.0) },
        )
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaAiPlugin(ttsCacheEngine = cache, outeTextToSpeechEngine = engine))
        }
        val executor = registry.nodeExecutors.resolve(MediaAiSpecs.textToSpeechOute.type)!!
        executor.execute(
            mapOf(
                "text" to WorkflowValue.StringValue("Hello"),
                "language" to WorkflowValue.StringValue("es"),
                "voice" to WorkflowValue.StringValue("narrator"),
                "instruct" to WorkflowValue.StringValue("cheerful tone"),
                "temperature" to WorkflowValue.DoubleValue(0.5),
                "seed" to WorkflowValue.IntValue(7),
            ),
        )
        val request = captured!!
        assertEquals("Hello", request.text)
        assertEquals("es", request.language)
        assertEquals("narrator", request.voiceId)
        assertEquals("cheerful tone", request.instruct)
        assertEquals(0.5, request.temperature)
        assertEquals(7, request.seed)
        assertEquals("oute", request.engineId)
    }

    @Test
    fun captionStyleProducesTypedRecord() = runTest {
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaAiPlugin()) }
        val executor = registry.nodeExecutors.resolve(MediaAiSpecs.captionStyle.type)!!
        val result = executor.execute(
            mapOf(
                "font_family" to WorkflowValue.StringValue("Arial"),
                "font_size" to WorkflowValue.IntValue(32),
                "text_color" to WorkflowValue.StringValue("#FFFFFF"),
                "background_color" to WorkflowValue.StringValue("#AA000000"),
                "outline_color" to WorkflowValue.StringValue("#000000"),
                "outline_width" to WorkflowValue.IntValue(2),
                "shadow" to WorkflowValue.IntValue(0),
                "bold" to WorkflowValue.BooleanValue(true),
                "italic" to WorkflowValue.BooleanValue(false),
                "alignment" to WorkflowValue.StringValue("BottomCenter"),
                "margin_horizontal" to WorkflowValue.IntValue(40),
                "margin_vertical" to WorkflowValue.IntValue(60),
            ),
        )
        val style = result["style_config"] as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.IntValue(32), style.fields["font_size"])
    }
}
