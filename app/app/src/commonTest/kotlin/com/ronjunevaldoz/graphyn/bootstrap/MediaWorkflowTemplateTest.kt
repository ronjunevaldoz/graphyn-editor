package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaWorkflowTemplateTest {

    @Test
    fun allMediaTemplatesHaveDedicatedCoverage() {
        val mediaScenes = WorkflowCatalog.entries
            .filter { scene -> scene.workflow.nodes.any { it.type.startsWith("media.") } }
            .toSet()

        assertEquals(
            setOf(
                WorkflowCatalog.SimpleTts,
                WorkflowCatalog.VideoNarration,
                WorkflowCatalog.AudioMix,
                WorkflowCatalog.SmartEncode,
                WorkflowCatalog.VideoStitch,
                WorkflowCatalog.Captioned,
                WorkflowCatalog.OcrExtract,
            ),
            mediaScenes,
        )
    }

    @Test
    fun simpleTextToSpeechTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.SimpleTts.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "simple-tts",
            expectedName = "Text to Speech",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolvePath" to "io.resolve_path",
                "text" to "io.file_read",
                "tts" to "media.text_to_speech",
                "preview" to "preview.view",
            ),
            expectedConnections = setOf(
                connection("resolvePath", "resolved_path", "text", "path"),
                connection("text", "content", "tts", "text"),
                connection("tts", "audio", "preview", "value"),
            ),
        )
        assertConfig(
            workflow.node("resolvePath"),
            "base_dir" to stringValue(MEDIA_RESOURCES_DIR),
            "relative_path" to stringValue("input.txt"),
        )
        assertConfig(
            workflow.node("tts"),
            "language" to stringValue("en"),
            "voice_id" to stringValue("default"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )
    }

    @Test
    fun videoNarrationTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.VideoNarration.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "video-narration",
            expectedName = "Video Narration",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolveVideo" to "io.resolve_path",
                "resolveText" to "io.resolve_path",
                "import_video" to "media.video_import",
                "narration_text" to "io.file_read",
                "extract_audio" to "media.audio_extract",
                "synthesize" to "media.text_to_speech",
                "collect_audio" to "media.audios_list",
                "mix_audio" to "media.audio_mix",
                "encode" to "media.video_encode",
                "output" to "media.file_output",
            ),
            expectedConnections = setOf(
                connection("resolveVideo", "resolved_path", "import_video", "path"),
                connection("resolveText", "resolved_path", "narration_text", "path"),
                connection("import_video", "video", "extract_audio", "video"),
                connection("extract_audio", "audio", "collect_audio", "audio1"),
                connection("narration_text", "content", "synthesize", "text"),
                connection("synthesize", "audio", "collect_audio", "audio2"),
                connection("collect_audio", "audios", "mix_audio", "audio_tracks"),
                connection("import_video", "video", "encode", "video"),
                connection("mix_audio", "audio", "encode", "audio"),
                connection("encode", "file_path", "output", "file_path"),
            ),
        )
        assertResolver(workflow.node("resolveVideo"), "input.mp4")
        assertResolver(workflow.node("resolveText"), "narration.txt")
        assertConfig(
            workflow.node("synthesize"),
            "language" to stringValue("en"),
            "voice_id" to stringValue("narrator"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )
        assertConfig(
            workflow.node("encode"),
            "output_path" to stringValue("output.mp4"),
            "bitrate" to stringValue("high"),
            "codec" to stringValue("h264"),
        )
    }

    @Test
    fun audioMixTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.AudioMix.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "audio-mix",
            expectedName = "Audio Mix",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolveVideo" to "io.resolve_path",
                "import_video" to "media.video_import",
                "background" to "media.audio_extract",
                "foreground" to "media.text_to_speech",
                "collect" to "media.audios_list",
                "mix" to "media.audio_mix",
                "caption_style" to "media.caption_style",
                "preview" to "preview.view",
            ),
            expectedConnections = setOf(
                connection("resolveVideo", "resolved_path", "import_video", "path"),
                connection("import_video", "video", "background", "video"),
                connection("background", "audio", "collect", "audio1"),
                connection("foreground", "audio", "collect", "audio2"),
                connection("collect", "audios", "mix", "audio_tracks"),
                connection("mix", "audio", "preview", "value"),
            ),
        )
        assertResolver(workflow.node("resolveVideo"), "input.mp4")
        assertConfig(
            workflow.node("foreground"),
            "language" to stringValue("en"),
            "voice_id" to stringValue("speaker"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )
        assertConfig(
            workflow.node("caption_style"),
            "color" to stringValue("#FFFFFF"),
            "background_color" to stringValue("#000000"),
            "font_size" to WorkflowValue.IntValue(24),
            "position" to stringValue("bottom"),
        )
    }

    @Test
    fun smartEncodeTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.SmartEncode.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "smart-encode",
            expectedName = "Smart Video Encode",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolvePath" to "io.resolve_path",
                "import" to "media.video_import",
                "decide" to "script.eval",
                "encode" to "media.video_encode",
                "output" to "media.file_output",
            ),
            expectedConnections = setOf(
                connection("resolvePath", "resolved_path", "import", "path"),
                connection("import", "duration_ms", "decide", "input"),
                connection("import", "video", "encode", "video"),
                connection("encode", "file_path", "output", "file_path"),
            ),
        )
        assertResolver(workflow.node("resolvePath"), "input.mp4")
        assertConfig(
            workflow.node("encode"),
            "output_path" to stringValue("smart_encoded.mp4"),
        )
        val script = workflow.node("decide").config["code"] as? WorkflowValue.StringValue
        assertTrue(script?.value?.contains("\"bitrate\" to bitrate") == true)
    }

    @Test
    fun videoStitchTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.VideoStitch.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "video-stitch",
            expectedName = "Video Stitch",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolvePath1" to "io.resolve_path",
                "resolvePath2" to "io.resolve_path",
                "import1" to "media.video_import",
                "import2" to "media.video_import",
                "collect" to "media.videos_list",
                "stitch" to "media.video_stitch",
                "encode" to "media.video_encode",
                "output" to "media.file_output",
            ),
            expectedConnections = setOf(
                connection("resolvePath1", "resolved_path", "import1", "path"),
                connection("resolvePath2", "resolved_path", "import2", "path"),
                connection("import1", "video", "collect", "video1"),
                connection("import2", "video", "collect", "video2"),
                connection("collect", "videos", "stitch", "videos"),
                connection("stitch", "video", "encode", "video"),
                connection("encode", "file_path", "output", "file_path"),
            ),
        )
        assertResolver(workflow.node("resolvePath1"), "clip1.mp4")
        assertResolver(workflow.node("resolvePath2"), "clip2.mp4")
        assertConfig(
            workflow.node("stitch"),
            "transition" to stringValue("cut"),
        )
        assertConfig(
            workflow.node("encode"),
            "output_path" to stringValue("stitched.mp4"),
            "bitrate" to stringValue("high"),
        )
    }

    @Test
    fun captionedVideoTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.Captioned.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "captioned-video",
            expectedName = "Captioned Video",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolveVideo" to "io.resolve_path",
                "import_video" to "media.video_import",
                "extract_audio" to "media.audio_extract",
                "transcribe" to "media.speech_to_text",
                "caption_style" to "media.caption_style",
                "caption_overlay" to "media.caption_overlay",
                "encode" to "media.video_encode",
                "output" to "media.file_output",
            ),
            expectedConnections = setOf(
                connection("resolveVideo", "resolved_path", "import_video", "path"),
                connection("import_video", "video", "extract_audio", "video"),
                connection("extract_audio", "audio", "transcribe", "audio"),
                connection("transcribe", "segments", "caption_overlay", "captions"),
                connection("caption_style", "style_config", "caption_overlay", "style_config"),
                connection("import_video", "video", "caption_overlay", "video"),
                connection("caption_overlay", "video", "encode", "video"),
                connection("encode", "file_path", "output", "file_path"),
            ),
        )
        assertResolver(workflow.node("resolveVideo"), "input.mp4")
        assertConfig(
            workflow.node("encode"),
            "output_path" to stringValue("captioned.mp4"),
            "bitrate" to stringValue("high"),
        )
    }

    @Test
    fun documentOcrTemplateIsConfiguredAndWired() {
        val workflow = WorkflowCatalog.OcrExtract.workflow

        assertTemplate(
            workflow = workflow,
            expectedId = "document-ocr",
            expectedName = "Document Text Extract",
            expectedNodes = mapOf(
                "guide" to "graphyn.sticky_note",
                "resolveImage" to "io.resolve_path",
                "import_image" to "media.image_import",
                "ocr" to "media.ocr",
                "preview" to "preview.view",
            ),
            expectedConnections = setOf(
                connection("resolveImage", "resolved_path", "import_image", "path"),
                connection("import_image", "image", "ocr", "image"),
                connection("ocr", "text", "preview", "value"),
            ),
        )
        assertResolver(workflow.node("resolveImage"), "sample.png")
    }

    private fun assertTemplate(
        workflow: WorkflowDefinition,
        expectedId: String,
        expectedName: String,
        expectedNodes: Map<String, String>,
        expectedConnections: Set<ConnectionRef>,
    ) {
        assertEquals(expectedId, workflow.id)
        assertEquals(expectedName, workflow.name)
        assertEquals(expectedNodes, workflow.nodes.associate { it.id to it.type })
        assertEquals(expectedConnections, workflow.connections.toSet())
        assertEquals(
            workflow.connections.size,
            workflow.connections.toSet().size,
            "${workflow.name} contains duplicate connections",
        )
    }

    private fun assertResolver(node: NodeRef, relativePath: String) {
        assertConfig(
            node,
            "base_dir" to stringValue(MEDIA_RESOURCES_DIR),
            "relative_path" to stringValue(relativePath),
        )
    }

    private fun assertConfig(node: NodeRef, vararg expected: Pair<String, WorkflowValue>) {
        assertEquals(expected.toMap(), node.config, "Unexpected config for node '${node.id}'")
    }

    private fun WorkflowDefinition.node(id: String): NodeRef =
        nodes.single { it.id == id }

    private fun connection(
        fromNodeId: String,
        fromPort: String,
        toNodeId: String,
        toPort: String,
    ) = ConnectionRef(fromNodeId, fromPort, toNodeId, toPort)

    private fun stringValue(value: String) = WorkflowValue.StringValue(value)

    private companion object {
        const val MEDIA_RESOURCES_DIR = "../../app/app/src/commonMain/resources/media"
    }
}
