@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MediaCorePluginTest {
    @Test
    fun registersAllSpecsAndExecutors() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(MediaCorePlugin(FakeMediaCoreBackend()))

        assertEquals(20, registry.nodeSpecs.all().size)
        (MediaCoreSpecs.all + MediaCompositionSpecs.all + MediaBuilderSpecs.all + MediaImageSpecs.all).forEach {
            assertNotNull(registry.nodeSpecs.resolve(it.type))
            assertNotNull(registry.nodeExecutors.resolve(it.type))
        }
    }

    @Test
    fun overlayBuilderAndCollectorFeedVideoCompose() = runTest {
        val backend = FakeMediaCoreBackend()
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaCorePlugin(backend)) }

        val built = registry.nodeExecutors.resolve(MediaBuilderSpecs.videoOverlay.type)!!.execute(
            mapOf(
                "source" to MediaTypes.videoValue("/media/logo.mp4"),
                "x" to WorkflowValue.IntValue(12),
                "y" to WorkflowValue.IntValue(34),
                "start_ms" to WorkflowValue.DoubleValue(0.0),
                "end_ms" to WorkflowValue.DoubleValue(500.0),
                "opacity" to WorkflowValue.DoubleValue(0.5),
            ),
        )
        val collected = registry.nodeExecutors.resolve(MediaBuilderSpecs.overlaysList.type)!!.execute(
            mapOf("overlay1" to built.getValue("overlay")),
        )
        registry.nodeExecutors.resolve(MediaCompositionSpecs.videoCompose.type)!!.execute(
            mapOf(
                "base_video" to MediaTypes.videoValue("/media/base.mp4"),
                "overlays" to collected.getValue("overlays"),
            ),
        )
        assertEquals(VideoOverlay("/media/logo.mp4", 12, 34, 0.0, 500.0, 0.5), backend.lastOverlays.single())
    }

    @Test
    fun syncPointBuilderAndCollectorFeedTimingController() = runTest {
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaCorePlugin(FakeMediaCoreBackend())) }

        suspend fun point(source: Double, target: Double) =
            registry.nodeExecutors.resolve(MediaBuilderSpecs.syncPoint.type)!!.execute(
                mapOf(
                    "source_ms" to WorkflowValue.DoubleValue(source),
                    "target_ms" to WorkflowValue.DoubleValue(target),
                ),
            ).getValue("point")

        val collected = registry.nodeExecutors.resolve(MediaBuilderSpecs.syncPointsList.type)!!.execute(
            mapOf("point1" to point(0.0, 100.0), "point2" to point(1000.0, 1200.0)),
        )
        val config = registry.nodeExecutors.resolve(MediaCompositionSpecs.timingController.type)!!.execute(
            mapOf(
                "base_video" to MediaTypes.videoValue("/media/base.mp4"),
                "sync_points" to collected.getValue("sync_points"),
            ),
        )
        val record = config.getValue("config") as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.DoubleValue(150.0), record.fields["audio_delay_ms"])
    }

    @Test
    fun captionOverlayAndComposePassTypedHandlesToBackend() = runTest {
        val backend = FakeMediaCoreBackend()
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaCorePlugin(backend)) }

        registry.nodeExecutors.resolve(MediaCompositionSpecs.captionOverlay.type)!!.execute(
            mapOf(
                "video" to MediaTypes.videoValue("/media/base.mp4"),
                "captions" to MediaCompositionTypes.captionList(
                    listOf(Triple("Hello", 0.0, 1000.0), Triple("World", 1000.0, 2000.0)),
                ),
                "style_config" to WorkflowValue.RecordValue(
                    mapOf(
                        "color" to WorkflowValue.StringValue("#FFFFFF"),
                        "background_color" to WorkflowValue.StringValue("#000000"),
                        "font_size" to WorkflowValue.IntValue(24),
                        "position" to WorkflowValue.StringValue("bottom"),
                    ),
                ),
            ),
        )
        assertEquals(listOf("Hello", "World"), backend.lastCaptions.map { it.text })

        registry.nodeExecutors.resolve(MediaCompositionSpecs.videoCompose.type)!!.execute(
            mapOf(
                "base_video" to MediaTypes.videoValue("/media/base.mp4"),
                "overlays" to WorkflowValue.ListValue(
                    listOf(
                        WorkflowValue.RecordValue(
                            mapOf(
                                "source" to MediaTypes.videoValue("/media/logo.mp4"),
                                "x" to WorkflowValue.IntValue(10),
                                "y" to WorkflowValue.IntValue(20),
                                "start_ms" to WorkflowValue.DoubleValue(0.0),
                                "end_ms" to WorkflowValue.DoubleValue(500.0),
                                "opacity" to WorkflowValue.DoubleValue(0.5),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(listOf("/media/logo.mp4"), backend.lastOverlays.map { it.sourcePath })
        assertEquals(0.5, backend.lastOverlays.single().opacity)
    }

    @Test
    fun imageOpsResizeCollectAndSequence() = runTest {
        val backend = FakeMediaCoreBackend()
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaCorePlugin(backend)) }

        registry.nodeExecutors.resolve(MediaImageSpecs.imageResize.type)!!.execute(
            mapOf(
                "image" to MediaTypes.imageValue("/media/a.png"),
                "width" to WorkflowValue.IntValue(800),
                "height" to WorkflowValue.IntValue(600),
            ),
        )
        assertEquals(800 to 600, backend.lastResize)

        val collected = registry.nodeExecutors.resolve(MediaImageSpecs.imagesList.type)!!.execute(
            mapOf(
                "image1" to MediaTypes.imageValue("/media/a.png"),
                "image2" to MediaTypes.imageValue("/media/b.png"),
            ),
        )
        registry.nodeExecutors.resolve(MediaImageSpecs.imageSequenceToVideo.type)!!.execute(
            mapOf("images" to collected.getValue("images"), "fps" to WorkflowValue.DoubleValue(2.0)),
        )
        assertEquals(2.0, backend.lastSequenceFps)
    }

    @Test
    fun audioEncodeWritesRequestedFormat() = runTest {
        val backend = FakeMediaCoreBackend()
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaCorePlugin(backend)) }

        val encoded = registry.nodeExecutors.resolve(MediaCompositionSpecs.audioEncode.type)!!.execute(
            mapOf(
                "audio" to MediaTypes.audioValue("/media/voice.wav"),
                "output_path" to WorkflowValue.StringValue("/out/voice.mp3"),
                "format" to WorkflowValue.StringValue("mp3"),
            ),
        )
        assertEquals("mp3", backend.lastAudioFormat)
        assertEquals(WorkflowValue.StringValue("/out/voice.mp3"), encoded["file_path"])
    }

    @Test
    fun timingControllerAveragesSyncPoints() = runTest {
        val registry = DefaultGraphynPluginRegistry().apply { install(MediaCorePlugin(FakeMediaCoreBackend())) }
        val config = registry.nodeExecutors.resolve(MediaCompositionSpecs.timingController.type)!!.execute(
            mapOf(
                "base_video" to MediaTypes.videoValue("/media/base.mp4"),
                "sync_points" to WorkflowValue.ListValue(
                    listOf(
                        syncPoint(source = 0.0, target = 100.0),
                        syncPoint(source = 1000.0, target = 1200.0),
                    ),
                ),
            ),
        )
        val record = config.getValue("config") as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.DoubleValue(150.0), record.fields["audio_delay_ms"])
        assertEquals(WorkflowValue.DoubleValue(150.0), record.fields["caption_offset_ms"])
        assertEquals(WorkflowValue.DoubleValue(0.0), record.fields["video_delay_ms"])
    }

    private fun syncPoint(source: Double, target: Double) = WorkflowValue.RecordValue(
        mapOf(
            "source_ms" to WorkflowValue.DoubleValue(source),
            "target_ms" to WorkflowValue.DoubleValue(target),
        ),
    )

    @Test
    fun audioAndVideoHandlesAreNotConnectionCompatible() {
        assertFalse(WorkflowTypeCompatibility.isCompatible(MediaTypes.videoHandle, MediaTypes.audioHandle))
        assertFalse(WorkflowTypeCompatibility.isCompatible(MediaTypes.audioHandle, MediaTypes.videoHandle))
    }

    @Test
    fun executorsMapTypedInputsAndOutputs() = runTest {
        val backend = FakeMediaCoreBackend()
        val registry = DefaultGraphynPluginRegistry().apply {
            install(MediaCorePlugin(backend))
        }

        val imported = registry.nodeExecutors.resolve(MediaCoreSpecs.videoImport.type)!!.execute(
            mapOf("path" to WorkflowValue.StringValue("/media/input.mp4")),
        )
        assertEquals(WorkflowValue.IntValue(1920), imported["width"])
        assertEquals("/media/input.mp4", MediaTypes.path(imported["video"], "video"))

        val mixed = registry.nodeExecutors.resolve(MediaCoreSpecs.audioMix.type)!!.execute(
            mapOf(
                "audio_tracks" to WorkflowValue.ListValue(
                    listOf(
                        MediaTypes.audioValue("/media/a.wav"),
                        MediaTypes.audioValue("/media/b.wav"),
                    ),
                ),
                "volumes" to WorkflowValue.ListValue(
                    listOf(WorkflowValue.DoubleValue(1.0), WorkflowValue.DoubleValue(0.5)),
                ),
            ),
        )
        assertEquals(listOf("/media/a.wav", "/media/b.wav"), backend.lastAudioPaths)
        assertEquals(listOf(1.0, 0.5), backend.lastVolumes)
        assertIs<WorkflowValue.RecordValue>(mixed["audio"])

        val collected = registry.nodeExecutors.resolve(MediaCoreSpecs.videosList.type)!!.execute(
            mapOf(
                "video1" to MediaTypes.videoValue("/media/first.mp4"),
                "video2" to WorkflowValue.NullValue,
                "video3" to MediaTypes.videoValue("/media/third.mp4"),
            ),
        )
        val videos = assertIs<WorkflowValue.ListValue>(collected["videos"]).items
        assertEquals(
            listOf("/media/first.mp4", "/media/third.mp4"),
            videos.map { MediaTypes.path(it, "video") },
        )

        val collectedAudio = registry.nodeExecutors.resolve(MediaCoreSpecs.audiosList.type)!!.execute(
            mapOf(
                "audio1" to MediaTypes.audioValue("/media/voice.wav"),
                "audio2" to MediaTypes.audioValue("/media/music.wav"),
            ),
        )
        val audios = assertIs<WorkflowValue.ListValue>(collectedAudio["audios"]).items
        assertEquals(
            listOf("/media/voice.wav", "/media/music.wav"),
            audios.map { MediaTypes.path(it, "audio") },
        )

        val encoded = registry.nodeExecutors.resolve(MediaCoreSpecs.videoEncode.type)!!.execute(
            mapOf(
                "video" to MediaTypes.videoValue("/media/input.mp4"),
                "audio" to MediaTypes.audioValue("/media/a.wav"),
                "output_path" to WorkflowValue.StringValue("/media/output.mp4"),
                "bitrate" to WorkflowValue.StringValue("medium"),
                "codec" to WorkflowValue.StringValue("h264"),
            ),
        )
        assertEquals(WorkflowValue.StringValue("/media/output.mp4"), encoded["file_path"])
        assertEquals(WorkflowValue.DoubleValue(1234.0), encoded["size_bytes"])
        assertEquals("medium", backend.lastBitrate)
    }
}

private class FakeMediaCoreBackend : MediaCoreBackend {
    var lastAudioPaths: List<String> = emptyList()
    var lastVolumes: List<Double> = emptyList()
    var lastBitrate: String? = null
    var lastCaptions: List<Caption> = emptyList()
    var lastOverlays: List<VideoOverlay> = emptyList()

    override suspend fun inspectVideo(path: String) =
        VideoMetadata(path, width = 1920, height = 1080, durationMs = 1_000.0, fps = 30.0, frameCount = 30)

    override suspend fun inspectImage(path: String) = ImageMetadata(path, width = 640, height = 480)

    override suspend fun extractAudio(videoPath: String) =
        AudioMetadata("/media/extracted.wav", sampleRate = 48_000, durationMs = 1_000.0)

    override suspend fun mixAudio(audioPaths: List<String>, volumes: List<Double>): AudioMetadata {
        lastAudioPaths = audioPaths
        lastVolumes = volumes
        return AudioMetadata("/media/mixed.wav", sampleRate = 48_000, durationMs = 1_000.0)
    }

    override suspend fun stitchVideos(videoPaths: List<String>) =
        VideoMetadata("/media/stitched.mp4", 1920, 1080, 2_000.0, 30.0, 60)

    override suspend fun encodeVideo(
        videoPath: String,
        audioPath: String?,
        outputPath: String,
        bitrate: String,
        codec: String,
    ): EncodedVideo {
        lastBitrate = bitrate
        return EncodedVideo(outputPath, sizeBytes = 1234, durationMs = 1_000.0)
    }

    override suspend fun overlayCaptions(
        videoPath: String,
        captions: List<Caption>,
        style: CaptionStyle,
    ): VideoMetadata {
        lastCaptions = captions
        return VideoMetadata("/media/captioned.mp4", 1920, 1080, 2_000.0, 30.0, 60)
    }

    override suspend fun composeVideo(
        baseVideoPath: String,
        overlays: List<VideoOverlay>,
    ): VideoMetadata {
        lastOverlays = overlays
        return VideoMetadata("/media/composed.mp4", 1920, 1080, 2_000.0, 30.0, 60)
    }

    var lastAudioFormat: String? = null
    var lastResize: Pair<Int, Int>? = null
    var lastSequenceFps: Double = 0.0

    override suspend fun encodeAudio(audioPath: String, outputPath: String, format: String): EncodedAudio {
        lastAudioFormat = format
        return EncodedAudio(outputPath, sizeBytes = 2048, durationMs = 3_000.0)
    }

    override suspend fun resizeImage(imagePath: String, width: Int, height: Int): ImageMetadata {
        lastResize = width to height
        return ImageMetadata("/media/resized.png", width, height)
    }

    override suspend fun cropImage(imagePath: String, x: Int, y: Int, width: Int, height: Int): ImageMetadata =
        ImageMetadata("/media/cropped.png", width, height)

    override suspend fun imageSequenceToVideo(imagePaths: List<String>, fps: Double): VideoMetadata {
        lastSequenceFps = fps
        return VideoMetadata("/media/slideshow.mp4", 1280, 720, imagePaths.size / fps * 1000.0, fps, imagePaths.size)
    }
}
