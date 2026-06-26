package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesSpecs

internal val styleNodesDemoWorkflow = WorkflowDefinition(
    id = "style-demo", name = "Style Demo",
    nodes = listOf(
        NodeRef("webhook", StyleNodesSpecs.webhook.type),
        NodeRef("sampler", StyleNodesSpecs.kSampler.type),
        NodeRef("scatter", StyleNodesSpecs.distributePoints.type),
    ),
    connections = listOf(ConnectionRef("sampler", "latent", "scatter", "mesh")),
)

internal val listOpsDemoWorkflow = WorkflowDefinition(
    id = "list-ops-demo", name = "List Ops",
    nodes = listOf(
        NodeRef("zip",    "listops.zip"),
        NodeRef("map",    "listops.map"),
        NodeRef("filter", "listops.filter"),
        NodeRef("reduce", "listops.reduce"),
    ),
    connections = listOf(
        ConnectionRef("zip",    "result", "map",    "list"),
        ConnectionRef("map",    "result", "filter", "list"),
        ConnectionRef("filter", "result", "reduce", "list"),
    ),
)

internal val controlDemoWorkflow = WorkflowDefinition(
    id = "control-demo", name = "Control Flow",
    nodes = listOf(
        NodeRef("loop",   "control.loop"),
        NodeRef("branch", "control.branch"),
        NodeRef("merge",  "control.merge"),
    ),
    connections = listOf(
        ConnectionRef("loop",   "item",      "branch", "value"),
        ConnectionRef("branch", "truePath",  "merge",  "a"),
        ConnectionRef("branch", "falsePath", "merge",  "b"),
    ),
)

internal val textDemoWorkflow = WorkflowDefinition(
    id = "text-demo", name = "Text Ops",
    nodes = listOf(
        NodeRef("format", "text.format"),
        NodeRef("split",  "text.split"),
        NodeRef("regex",  "text.regex"),
    ),
    connections = listOf(ConnectionRef("format", "result", "split", "text")),
)

internal val typesDemoWorkflow = WorkflowDefinition(
    id = "types-demo", name = "Type Utils",
    nodes = listOf(
        NodeRef("schema",   "types.schema"),
        NodeRef("cast",     "types.cast"),
        NodeRef("validate", "types.validate"),
    ),
    connections = listOf(
        ConnectionRef("schema", "schema", "validate", "schema"),
        ConnectionRef("cast",   "result", "validate", "value"),
    ),
)

internal val ioDemoWorkflow = WorkflowDefinition(
    id = "io-demo", name = "I/O",
    nodes = listOf(
        NodeRef("request", "io.http_request"),
        NodeRef("read",    "io.file_read"),
        NodeRef("write",   "io.file_write"),
    ),
    connections = listOf(ConnectionRef("request", "body", "write", "content")),
)

private val subgraphInnerWorkflow = WorkflowDefinition(
    id = "subgraph-inner", name = "File Copy Pipeline",
    nodes = listOf(
        NodeRef("browse", "io.file_browse"),
        NodeRef("read",   "io.file_read"),
        NodeRef("sink",   "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("browse", "path",    "read",  "path"),
        ConnectionRef("read",   "content", "sink",  "content"),
    ),
)

internal val subgraphDemoWorkflow = WorkflowDefinition(
    id = "subgraph-demo", name = "Subgraph",
    nodes = listOf(
        NodeRef("src",      "io.file_browse"),
        NodeRef("pipeline", SUBGRAPH_NODE_TYPE, subgraph = subgraphInnerWorkflow),
        NodeRef("out_dir",  "io.folder_browse"),
        NodeRef("write",    "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("src",      "path",   "pipeline", "input"),
        ConnectionRef("out_dir",  "path",   "write",    "path"),
        ConnectionRef("pipeline", "output", "write",    "content"),
    ),
)

/**
 * Production-shaped API ingestion pipeline: fetch a JSON document over HTTP, parse it,
 * extract fields by path, re-serialize, and persist to a file.
 *
 * Demonstrates the canonical fetch → parse → transform → persist flow and, with the
 * resilient executor, live per-node status plus partial results when a node fails
 * (e.g. no network: `fetch` errors, downstream nodes are skipped, independent ones still run).
 */
internal val apiIngestionDemoWorkflow = WorkflowDefinition(
    id = "api-ingestion-demo", name = "API Ingestion",
    nodes = listOf(
        NodeRef("fetch", "io.http_request", config = mapOf(
            "url" to WorkflowValue.StringValue("https://api.github.com/repos/JetBrains/kotlin"),
            "method" to WorkflowValue.StringValue("GET"),
        )),
        NodeRef("parse", "json.parse"),
        NodeRef("stars", "json.path", config = mapOf("path" to WorkflowValue.StringValue("stargazers_count"))),
        NodeRef("forks", "json.path", config = mapOf("path" to WorkflowValue.StringValue("forks_count"))),
        NodeRef("pretty", "json.stringify", config = mapOf("pretty" to WorkflowValue.BooleanValue(true))),
        NodeRef("save", "io.file_write", config = mapOf("path" to WorkflowValue.StringValue("repo.json"))),
    ),
    connections = listOf(
        ConnectionRef("fetch",  "body",  "parse",  "text"),
        ConnectionRef("parse",  "value", "stars",  "value"),
        ConnectionRef("parse",  "value", "forks",  "value"),
        ConnectionRef("parse",  "value", "pretty", "value"),
        ConnectionRef("pretty", "text",  "save",   "content"),
    ),
)

// Media sample fixtures live under app/app resources; paths resolve via io.resolve_path so they
// work regardless of the process working directory.
internal const val MEDIA_DIR = "../../app/app/src/commonMain/resources/media"

/**
 * On-canvas guide note shown beside each media template. Auto-layout parks annotation nodes in a
 * column to the left of the graph, so this reads as a legend.
 */
internal fun guideNote(text: String, width: Int = 300, height: Int = 240): NodeRef = NodeRef(
    "guide", "graphyn.sticky_note",
    config = mapOf(
        "text" to WorkflowValue.StringValue(text.trimIndent()),
        "__w" to WorkflowValue.IntValue(width),
        "__h" to WorkflowValue.IntValue(height),
    ),
)

/**
 * Simple text-to-speech workflow.
 *
 * Convert text to audio using the TTS engine, cache the result, and optionally save.
 * Demonstrates the media AI suite and credential/environment variable integration.
 */
internal val simpleTtsWorkflow = WorkflowDefinition(
    id = "simple-tts", name = "Text to Speech",
    nodes = listOf(
        guideNote(
            """
            Text to Speech

            Reads a text file and synthesizes spoken audio, then saves a WAV.

            Flow: Resolve Path → File Read → Text to Speech → Audio Encode
            → Media Output.
            Use cases: voiceover, narration drafts, accessibility audio.
            Tips: set voice_id/speed on the TTS node; results are cached.
            """,
        ),
        NodeRef("resolvePath", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.txt"),
        )),
        NodeRef("text", "io.file_read"),
        NodeRef("tts", "media.text_to_speech", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("default"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )),
        NodeRef("encode", "media.audio_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("speech.wav"),
            "format" to WorkflowValue.StringValue("wav"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolvePath", "resolved_path", "text", "path"),
        ConnectionRef("text", "content", "tts", "text"),
        ConnectionRef("tts", "audio", "encode", "audio"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)

/**
 * Video with synthesized narration.
 *
 * Import a video, extract its audio, synthesize TTS narration, mix both tracks,
 * and re-encode the final video with both audio streams.
 *
 * Demonstrates chaining video and audio operations, TTS integration, and encoding.
 */
internal val videoNarrationWorkflow = WorkflowDefinition(
    id = "video-narration", name = "Video Narration",
    nodes = listOf(
        guideNote(
            """
            Video Narration

            Adds a synthesized narration track to a video.

            Flow: import video + read script → extract original audio +
            synthesize TTS → mix both tracks → encode → Media Output.
            Use cases: explainer videos, dubbed clips, auto voiceover.
            Tips: the audio mix balances the original track and narration.
            """,
            height = 260,
        ),
        NodeRef("resolveVideo", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.mp4"),
        )),
        NodeRef("resolveText", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("narration.txt"),
        )),
        NodeRef("import_video", "media.video_import"),
        NodeRef("narration_text", "io.file_read"),
        NodeRef("extract_audio", "media.audio_extract"),
        NodeRef("synthesize", "media.text_to_speech", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("narrator"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )),
        NodeRef("collect_audio", "media.audios_list"),
        NodeRef("mix_audio", "media.audio_mix"),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("output.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
            "codec" to WorkflowValue.StringValue("h264"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolveVideo", "resolved_path", "import_video", "path"),
        ConnectionRef("resolveText",  "resolved_path", "narration_text", "path"),
        ConnectionRef("import_video",   "video",  "extract_audio", "video"),
        ConnectionRef("extract_audio",  "audio",  "collect_audio", "audio1"),
        ConnectionRef("narration_text", "content", "synthesize",    "text"),
        ConnectionRef("synthesize",     "audio",  "collect_audio", "audio2"),
        ConnectionRef("collect_audio",  "audios", "mix_audio",     "audio_tracks"),
        ConnectionRef("import_video",   "video",  "encode",        "video"),
        ConnectionRef("mix_audio",      "audio",  "encode",        "audio"),
        ConnectionRef("encode",         "file_path", "output",     "file_path"),
    ),
)

/**
 * Audio mixing and styling.
 *
 * Combine multiple audio tracks, define caption style for later use,
 * and demonstrate metadata node usage alongside media operations.
 */
internal val audioMixWorkflow = WorkflowDefinition(
    id = "audio-mix", name = "Audio Mix",
    nodes = listOf(
        guideNote(
            """
            Audio Mix

            Blends a background track (extracted from a video) with a
            synthesized voice, and defines a caption style for later use.

            Flow: import video → extract audio + synthesize TTS →
            collect → mix → Audio Encode → Media Output.
            Use cases: podcasts, voiced slideshows, narrated b-roll.
            Tips: caption_style is a metadata node consumed downstream.
            """,
            height = 260,
        ),
        NodeRef("resolveVideo", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.mp4"),
        )),
        NodeRef("import_video", "media.video_import"),
        NodeRef("background", "media.audio_extract"),
        NodeRef("foreground", "media.text_to_speech", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("speaker"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )),
        NodeRef("collect", "media.audios_list"),
        NodeRef("mix", "media.audio_mix"),
        NodeRef("caption_style", "media.caption_style", config = mapOf(
            "color" to WorkflowValue.StringValue("#FFFFFF"),
            "background_color" to WorkflowValue.StringValue("#000000"),
            "font_size" to WorkflowValue.IntValue(24),
            "position" to WorkflowValue.StringValue("bottom"),
        )),
        NodeRef("encode", "media.audio_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("mixed.mp3"),
            "format" to WorkflowValue.StringValue("mp3"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolveVideo", "resolved_path", "import_video", "path"),
        ConnectionRef("import_video", "video", "background", "video"),
        ConnectionRef("background", "audio", "collect", "audio1"),
        ConnectionRef("foreground", "audio", "collect", "audio2"),
        ConnectionRef("collect", "audios", "mix", "audio_tracks"),
        ConnectionRef("mix", "audio", "encode", "audio"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)

/**
 * Smart video encoding with adaptive bitrate.
 *
 * Uses a Script node to analyze video duration and automatically choose
 * encoding bitrate (high for short clips, low for long videos).
 *
 * Demonstrates Script node integration with media workflows for
 * parameter calculation and decision logic.
 */
internal val smartEncodeWorkflow = WorkflowDefinition(
    id = "smart-encode", name = "Smart Video Encode",
    nodes = listOf(
        guideNote(
            """
            Smart Video Encode

            Picks an encoding bitrate from the source duration using a
            Kotlin Script node, then encodes.

            Flow: Resolve Path → Video Import → Script (decide bitrate) →
            Video Encode → Media Output.
            Use cases: adaptive exports, batch transcoding presets.
            Tips: edit the script to change the duration→bitrate rules.
            """,
            height = 260,
        ),
        NodeRef("resolvePath", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("input.mp4"),
        )),
        NodeRef("import", "media.video_import"),
        NodeRef("decide", "script.eval", config = mapOf(
            "code" to WorkflowValue.StringValue(
                $$"""
                // Auto-choose bitrate based on video duration
                val durationMs = input as? Double ?: 5000.0
                val durationMins = durationMs / 60000.0

                val bitrate = when {
                    durationMins < 2.0 -> "high"      // Short: high quality
                    durationMins < 15.0 -> "medium"   // Medium: balanced
                    else -> "low"                      // Long: space-saving
                }

                val formattedMins = String.format("%.1f", durationMins)

                mapOf(
                    "bitrate" to bitrate,
                    "message" to "Duration: $formattedMins" + "m → " + bitrate
                )
                """.trimIndent()
            )
        )),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("smart_encoded.mp4"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolvePath", "resolved_path", "import", "path"),
        ConnectionRef("import", "duration_ms", "decide", "input"),
        // Note: In real workflows, script result would be parsed to extract bitrate
        ConnectionRef("import", "video", "encode", "video"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)

/**
 * Video concatenation with cut transitions.
 *
 * Stitch multiple video clips together using hard cuts,
 * then export the final composite to MP4.
 *
 * Demonstrates video concatenation and encoding workflows.
 * Uses Videos List helper to collect clips into a list.
 */
internal val videoStitchWorkflow = WorkflowDefinition(
    id = "video-stitch", name = "Video Stitch",
    nodes = listOf(
        guideNote(
            """
            Video Stitch

            Concatenates multiple clips with hard cuts and exports one MP4.

            Flow: Resolve Path ×2 → Video Import ×2 → Videos List →
            Video Stitch → Video Encode → Media Output.
            Use cases: reels, montages, joining recorded segments.
            Tips: Videos List collects individual clips into the list the
            stitch node expects; add more clips via video3/video4.
            """,
            height = 260,
        ),
        NodeRef("resolvePath1", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("clip1.mp4"),
        )),
        NodeRef("resolvePath2", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("clip2.mp4"),
        )),
        NodeRef("import1", "media.video_import"),
        NodeRef("import2", "media.video_import"),
        NodeRef("collect", "media.videos_list"),
        NodeRef("stitch", "media.video_stitch", config = mapOf(
            "transition" to WorkflowValue.StringValue("cut"),
        )),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("stitched.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolvePath1", "resolved_path", "import1", "path"),
        ConnectionRef("resolvePath2", "resolved_path", "import2", "path"),
        ConnectionRef("import1", "video", "collect", "video1"),
        ConnectionRef("import2", "video", "collect", "video2"),
        ConnectionRef("collect", "videos", "stitch",  "videos"),
        ConnectionRef("stitch",  "video",  "encode",  "video"),
        ConnectionRef("encode",  "file_path", "output", "file_path"),
    ),
)
