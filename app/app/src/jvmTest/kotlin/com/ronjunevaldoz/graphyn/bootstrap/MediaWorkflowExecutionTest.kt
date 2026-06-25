package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCompositionTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
import java.util.Collections
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaWorkflowExecutionTest {

    @Test
    fun simpleTextToSpeechExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.SimpleTts)

        result.assertFullSuccess(expectedNodeCount = 5)
        assertEquals(listOf("Text from input.txt"), fixture.ttsTexts)
        assertEquals(
            "/generated/default.wav",
            MediaTypes.path(result.output("tts", "audio"), "audio"),
        )
    }

    @Test
    fun videoNarrationExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.VideoNarration)

        result.assertFullSuccess(expectedNodeCount = 11)
        assertEquals(listOf("Narration from narration.txt"), fixture.ttsTexts)
        assertEquals(
            listOf("/generated/input-extracted.wav", "/generated/narrator.wav"),
            fixture.lastAudioMixPaths,
        )
        assertEquals(
            EncodeCall(
                videoPath = "/fixtures/input.mp4",
                audioPath = "/generated/mixed.wav",
                outputPath = "output.mp4",
                bitrate = "high",
                codec = "h264",
            ),
            fixture.lastEncodeCall,
        )
        assertEquals(stringValue("output.mp4"), result.output("encode", "file_path"))
    }

    @Test
    fun audioMixExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.AudioMix)

        result.assertFullSuccess(expectedNodeCount = 9)
        assertEquals(
            listOf("/generated/input-extracted.wav", "/generated/speaker.wav"),
            fixture.lastAudioMixPaths,
        )
        assertEquals(
            WorkflowValue.IntValue(24),
            (result.output("caption_style", "style_config") as WorkflowValue.RecordValue)
                .fields["font_size"],
        )
    }

    @Test
    fun smartEncodeExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.SmartEncode)

        result.assertFullSuccess(expectedNodeCount = 6)
        assertEquals(
            listOf<WorkflowValue>(WorkflowValue.DoubleValue(90_000.0)),
            fixture.scriptInputs,
        )
        assertEquals(
            EncodeCall(
                videoPath = "/fixtures/input.mp4",
                audioPath = null,
                outputPath = "smart_encoded.mp4",
                bitrate = "high",
                codec = "h264",
            ),
            fixture.lastEncodeCall,
        )
        assertEquals(stringValue("smart_encoded.mp4"), result.output("encode", "file_path"))
    }

    @Test
    fun videoStitchExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.VideoStitch)

        result.assertFullSuccess(expectedNodeCount = 9)
        assertEquals(
            listOf("/fixtures/clip1.mp4", "/fixtures/clip2.mp4"),
            fixture.lastStitchPaths,
        )
        assertEquals(
            EncodeCall(
                videoPath = "/generated/stitched.mp4",
                audioPath = null,
                outputPath = "stitched.mp4",
                bitrate = "high",
                codec = "h264",
            ),
            fixture.lastEncodeCall,
        )
        assertEquals(stringValue("stitched.mp4"), result.output("output", "file_path"))
    }

    @Test
    fun captionedVideoExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.Captioned)

        result.assertFullSuccess(expectedNodeCount = 9)
        assertEquals(2, fixture.lastCaptionCount)
        assertEquals(
            EncodeCall(
                videoPath = "/generated/captioned.mp4",
                audioPath = null,
                outputPath = "captioned.mp4",
                bitrate = "high",
                codec = "h264",
            ),
            fixture.lastEncodeCall,
        )
        assertEquals(stringValue("captioned.mp4"), result.output("encode", "file_path"))
    }

    @Test
    fun documentOcrExecutesEndToEnd() = runTest {
        val fixture = MediaExecutionFixture()

        val result = fixture.execute(WorkflowCatalog.OcrExtract)

        result.assertFullSuccess(expectedNodeCount = 5)
        assertEquals("sample.png", fixture.lastOcrImage)
        assertEquals(stringValue("INVOICE"), result.output("ocr", "text"))
    }
}

private class MediaExecutionFixture {
    val ttsTexts: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val scriptInputs: MutableList<WorkflowValue> = Collections.synchronizedList(mutableListOf())
    var lastAudioMixPaths: List<String> = emptyList()
    var lastStitchPaths: List<String> = emptyList()
    var lastEncodeCall: EncodeCall? = null
    var lastCaptionCount: Int = 0
    var lastOcrImage: String? = null

    private val executors = DefaultNodeExecutorRegistry().apply {
        register("io.resolve_path") { inputs ->
            val relativePath = inputs.string("relative_path")
            mapOf("resolved_path" to stringValue("/fixtures/$relativePath"))
        }
        register("io.file_read") { inputs ->
            val fileName = inputs.string("path").substringAfterLast('/')
            val content = when (fileName) {
                "input.txt" -> "Text from input.txt"
                "narration.txt" -> "Narration from narration.txt"
                else -> "Text from $fileName"
            }
            mapOf(
                "content" to stringValue(content),
                "exists" to WorkflowValue.BooleanValue(true),
            )
        }
        register("media.video_import") { inputs ->
            val path = inputs.string("path")
            mapOf(
                "video" to MediaTypes.videoValue(path),
                "width" to WorkflowValue.IntValue(1920),
                "height" to WorkflowValue.IntValue(1080),
                "duration_ms" to WorkflowValue.DoubleValue(90_000.0),
                "fps" to WorkflowValue.DoubleValue(30.0),
            )
        }
        register("media.audio_extract") { inputs ->
            val sourcePath = inputs["video"]
                ?.let { MediaTypes.path(it, "video").substringAfterLast('/').substringBeforeLast('.') }
            val outputName = sourcePath?.let { "$it-extracted.wav" } ?: "extracted.wav"
            mapOf(
                "audio" to MediaTypes.audioValue("/generated/$outputName"),
                "sample_rate" to WorkflowValue.IntValue(48_000),
                "duration_ms" to WorkflowValue.DoubleValue(90_000.0),
            )
        }
        register("media.text_to_speech") { inputs ->
            val text = inputs.stringOrEmpty("text")
            val voice = inputs.stringOrDefault("voice_id", "default")
            ttsTexts += text
            mapOf(
                "audio" to MediaTypes.audioValue("/generated/$voice.wav"),
                "duration_ms" to WorkflowValue.DoubleValue(3_000.0),
                "cached" to WorkflowValue.BooleanValue(false),
            )
        }
        register("media.audios_list") { inputs ->
            mapOf("audios" to WorkflowValue.ListValue(inputs.mediaValues("audio")))
        }
        register("media.audio_mix") { inputs ->
            val audios = (inputs["audio_tracks"] as WorkflowValue.ListValue).items
            lastAudioMixPaths = audios.map { MediaTypes.path(it, "audio") }
            mapOf(
                "audio" to MediaTypes.audioValue("/generated/mixed.wav"),
                "duration_ms" to WorkflowValue.DoubleValue(90_000.0),
            )
        }
        register("media.caption_style") { inputs ->
            mapOf(
                "style_config" to WorkflowValue.RecordValue(
                    mapOf(
                        "color" to inputs.getValue("color"),
                        "background_color" to inputs.getValue("background_color"),
                        "font_size" to inputs.getValue("font_size"),
                        "position" to inputs.getValue("position"),
                    ),
                ),
            )
        }
        register("script.eval") { inputs ->
            scriptInputs += inputs.getValue("input")
            mapOf(
                "result" to WorkflowValue.RecordValue(
                    mapOf("bitrate" to stringValue("medium")),
                ),
            )
        }
        register("media.videos_list") { inputs ->
            mapOf("videos" to WorkflowValue.ListValue(inputs.mediaValues("video")))
        }
        register("media.video_stitch") { inputs ->
            val videos = (inputs["videos"] as WorkflowValue.ListValue).items
            lastStitchPaths = videos.map { MediaTypes.path(it, "video") }
            mapOf(
                "video" to MediaTypes.videoValue("/generated/stitched.mp4"),
                "duration_ms" to WorkflowValue.DoubleValue(180_000.0),
                "frame_count" to WorkflowValue.IntValue(5_400),
            )
        }
        register("media.video_encode") { inputs ->
            val outputPath = inputs.stringOrDefault("output_path", "output.mp4")
            lastEncodeCall = EncodeCall(
                videoPath = MediaTypes.path(inputs["video"], "video"),
                audioPath = inputs["audio"]
                    ?.takeUnless { it == WorkflowValue.NullValue }
                    ?.let { MediaTypes.path(it, "audio") },
                outputPath = outputPath,
                bitrate = inputs.stringOrDefault("bitrate", "high"),
                codec = inputs.stringOrDefault("codec", "h264"),
            )
            mapOf(
                "file_path" to stringValue(outputPath),
                "size_bytes" to WorkflowValue.DoubleValue(1_024.0),
                "duration_ms" to WorkflowValue.DoubleValue(90_000.0),
            )
        }
        register("media.file_output") { inputs ->
            mapOf("file_path" to inputs.getValue("file_path"))
        }
        register("preview.view") { inputs ->
            mapOf("value" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        register("media.speech_to_text") { inputs ->
            val audioName = MediaTypes.path(inputs["audio"], "audio").substringAfterLast('/')
            mapOf(
                "text" to stringValue("transcript of $audioName"),
                "confidence" to WorkflowValue.DoubleValue(0.9),
                "segments" to MediaCompositionTypes.captionList(
                    listOf(Triple("hello", 0.0, 1_000.0), Triple("world", 1_000.0, 2_000.0)),
                ),
            )
        }
        register("media.caption_overlay") { inputs ->
            lastCaptionCount = (inputs["captions"] as WorkflowValue.ListValue).items.size
            mapOf(
                "video" to MediaTypes.videoValue("/generated/captioned.mp4"),
                "duration_ms" to WorkflowValue.DoubleValue(90_000.0),
            )
        }
        register("media.image_import") { inputs ->
            mapOf(
                "image" to MediaTypes.imageValue(inputs.string("path")),
                "width" to WorkflowValue.IntValue(1024),
                "height" to WorkflowValue.IntValue(768),
            )
        }
        register("media.ocr") { inputs ->
            lastOcrImage = MediaTypes.path(inputs["image"], "image").substringAfterLast('/')
            mapOf(
                "text" to stringValue("INVOICE"),
                "blocks" to WorkflowValue.ListValue(emptyList()),
            )
        }
        // Annotation node: no data ports, executes as a no-op (mirrors StickyNotePlugin).
        register("graphyn.sticky_note") { emptyMap() }
    }

    suspend fun execute(scene: WorkflowCatalog): WorkflowExecutionResult =
        WorkflowExecutionEngine(executors).execute(scene.workflow)
}

private data class EncodeCall(
    val videoPath: String,
    val audioPath: String?,
    val outputPath: String,
    val bitrate: String,
    val codec: String,
)

private fun WorkflowExecutionResult.assertFullSuccess(expectedNodeCount: Int) {
    assertTrue(isFullSuccess, "Execution failed: $statusByNodeId, errors: $errorsByNodeId")
    assertEquals(expectedNodeCount, successCount)
}

private fun WorkflowExecutionResult.output(nodeId: String, port: String): WorkflowValue =
    nodeOutputsByNodeId.getValue(nodeId).getValue(port)

private fun Map<String, WorkflowValue>.mediaValues(prefix: String): List<WorkflowValue> =
    entries
        .filter { (key, value) -> key.startsWith(prefix) && value != WorkflowValue.NullValue }
        .sortedBy { it.key }
        .map { it.value }

private fun Map<String, WorkflowValue>.string(key: String): String =
    (getValue(key) as WorkflowValue.StringValue).value

private fun Map<String, WorkflowValue>.stringOrEmpty(key: String): String =
    (get(key) as? WorkflowValue.StringValue)?.value.orEmpty()

private fun Map<String, WorkflowValue>.stringOrDefault(key: String, default: String): String =
    (get(key) as? WorkflowValue.StringValue)?.value ?: default

private fun stringValue(value: String) = WorkflowValue.StringValue(value)
