package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

class MediaCorePlugin(
    private val backend: MediaCoreBackend = FfmpegMediaCoreBackend(),
) : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.media.core",
        displayName = "Media Core",
        version = "0.4.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        MediaCoreSpecs.all.forEach(registrar::registerNodeSpec)
        registrar.registerExecutor(MediaCoreSpecs.videoImport.type, videoImportExecutor())
        registrar.registerExecutor(MediaCoreSpecs.audioExtract.type, audioExtractExecutor())
        registrar.registerExecutor(MediaCoreSpecs.audioMix.type, audioMixExecutor())
        registrar.registerExecutor(MediaCoreSpecs.audiosList.type, mediaListExecutor("audio", "audios"))
        registrar.registerExecutor(MediaCoreSpecs.videosList.type, mediaListExecutor("video", "videos"))
        registrar.registerExecutor(MediaCoreSpecs.videoStitch.type, videoStitchExecutor())
        registrar.registerExecutor(MediaCoreSpecs.videoEncode.type, videoEncodeExecutor())
    }

    private fun videoImportExecutor() = NodeExecutor { inputs ->
        val metadata = backend.inspectVideo(inputs.string("path"))
        mapOf(
            "video" to MediaTypes.videoValue(metadata.path),
            "width" to WorkflowValue.IntValue(metadata.width),
            "height" to WorkflowValue.IntValue(metadata.height),
            "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
            "fps" to WorkflowValue.DoubleValue(metadata.fps),
        )
    }

    private fun audioExtractExecutor() = NodeExecutor { inputs ->
        val metadata = backend.extractAudio(MediaTypes.path(inputs["video"], "video"))
        mapOf(
            "audio" to MediaTypes.audioValue(metadata.path),
            "sample_rate" to WorkflowValue.IntValue(metadata.sampleRate),
            "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
        )
    }

    private fun audioMixExecutor() = NodeExecutor { inputs ->
        val audioPaths = inputs.list("audio_tracks").map { MediaTypes.path(it, "audio") }
        val volumes = inputs.listOrEmpty("volumes").map {
            when (it) {
                is WorkflowValue.DoubleValue -> it.value
                is WorkflowValue.IntValue -> it.value.toDouble()
                else -> error("Audio Mix volumes must be numbers.")
            }
        }
        val metadata = backend.mixAudio(audioPaths, volumes)
        mapOf(
            "audio" to MediaTypes.audioValue(metadata.path),
            "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
        )
    }

    private fun mediaListExecutor(handleType: String, outputName: String) = NodeExecutor { inputs ->
        val handles = (1..4).mapNotNull { index ->
            inputs["$handleType$index"]?.takeUnless { it == WorkflowValue.NullValue }
        }
        require(handles.isNotEmpty()) { "${handleType.replaceFirstChar(Char::uppercase)}s List requires at least one $handleType." }
        handles.forEach { MediaTypes.path(it, handleType) }
        mapOf(outputName to WorkflowValue.ListValue(handles))
    }

    private fun videoStitchExecutor() = NodeExecutor { inputs ->
        require(inputs.stringOr("transition", "cut") == "cut") { "Phase 1 supports only cut transitions." }
        val videoPaths = inputs.list("videos").map { MediaTypes.path(it, "video") }
        val metadata = backend.stitchVideos(videoPaths)
        mapOf(
            "video" to MediaTypes.videoValue(metadata.path),
            "duration_ms" to WorkflowValue.DoubleValue(metadata.durationMs),
            "frame_count" to WorkflowValue.IntValue(metadata.frameCount),
        )
    }

    private fun videoEncodeExecutor() = NodeExecutor { inputs ->
        val audioPath = inputs["audio"]
            ?.takeUnless { it == WorkflowValue.NullValue }
            ?.let { MediaTypes.path(it, "audio") }
        val encoded = backend.encodeVideo(
            videoPath = MediaTypes.path(inputs["video"], "video"),
            audioPath = audioPath,
            outputPath = inputs.string("output_path"),
            bitrate = inputs.stringOr("bitrate", "high"),
            codec = inputs.stringOr("codec", "h264"),
        )
        mapOf(
            "file_path" to WorkflowValue.StringValue(encoded.path),
            "size_bytes" to WorkflowValue.DoubleValue(encoded.sizeBytes.toDouble()),
            "duration_ms" to WorkflowValue.DoubleValue(encoded.durationMs),
        )
    }
}

private fun Map<String, WorkflowValue>.string(key: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value
        ?.takeIf(String::isNotBlank)
        ?: error("Missing required string input '$key'.")

private fun Map<String, WorkflowValue>.stringOr(key: String, default: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value ?: default

private fun Map<String, WorkflowValue>.list(key: String): List<WorkflowValue> =
    (this[key] as? WorkflowValue.ListValue)?.items ?: error("Missing required list input '$key'.")

private fun Map<String, WorkflowValue>.listOrEmpty(key: String): List<WorkflowValue> =
    (this[key] as? WorkflowValue.ListValue)?.items.orEmpty()
