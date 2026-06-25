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

/**
 * Simple text-to-speech workflow.
 *
 * Convert text to audio using the TTS engine, cache the result, and optionally save.
 * Demonstrates the media AI suite and credential/environment variable integration.
 */
internal val simpleTtsWorkflow = WorkflowDefinition(
    id = "simple-tts", name = "Text to Speech",
    nodes = listOf(
        NodeRef("text", "io.file_read", config = mapOf(
            "path" to WorkflowValue.StringValue("input.txt")
        )),
        NodeRef("tts", "media.text_to_speech", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("default"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )),
    ),
    connections = listOf(
        ConnectionRef("text", "content", "tts", "text"),
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
        NodeRef("import_video", "media.video_import", config = mapOf(
            "path" to WorkflowValue.StringValue("input.mp4")
        )),
        NodeRef("narration_text", "io.file_read", config = mapOf(
            "path" to WorkflowValue.StringValue("narration.txt")
        )),
        NodeRef("extract_audio", "media.audio_extract"),
        NodeRef("synthesize", "media.text_to_speech", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("narrator"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )),
        NodeRef("mix_audio", "media.audio_mix"),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("output.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
            "codec" to WorkflowValue.StringValue("h264"),
        )),
    ),
    connections = listOf(
        ConnectionRef("import_video",   "video",  "extract_audio", "video"),
        ConnectionRef("extract_audio",  "audio",  "mix_audio",     "audio_tracks"),
        ConnectionRef("narration_text", "content", "synthesize",    "text"),
        ConnectionRef("synthesize",     "audio",  "mix_audio",     "audio_tracks"),
        ConnectionRef("import_video",   "video",  "encode",        "video"),
        ConnectionRef("mix_audio",      "audio",  "encode",        "audio"),
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
        NodeRef("background", "media.audio_extract"),
        NodeRef("foreground", "media.text_to_speech", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("speaker"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        )),
        NodeRef("mix", "media.audio_mix"),
        NodeRef("caption_style", "media.caption_style", config = mapOf(
            "color" to WorkflowValue.StringValue("#FFFFFF"),
            "background_color" to WorkflowValue.StringValue("#000000"),
            "font_size" to WorkflowValue.IntValue(24),
            "position" to WorkflowValue.StringValue("bottom"),
        )),
    ),
    connections = listOf(
        ConnectionRef("background", "audio", "mix", "audio_tracks"),
        ConnectionRef("foreground", "audio", "mix", "audio_tracks"),
    ),
)

/**
 * Video concatenation with cut transitions.
 *
 * Stitch multiple video clips together using hard cuts,
 * then export the final composite to MP4.
 *
 * Demonstrates video concatenation and encoding workflows.
 */
internal val videoStitchWorkflow = WorkflowDefinition(
    id = "video-stitch", name = "Video Stitch",
    nodes = listOf(
        NodeRef("import1", "media.video_import", config = mapOf(
            "path" to WorkflowValue.StringValue("clip1.mp4")
        )),
        NodeRef("import2", "media.video_import", config = mapOf(
            "path" to WorkflowValue.StringValue("clip2.mp4")
        )),
        NodeRef("stitch", "media.video_stitch", config = mapOf(
            "transition" to WorkflowValue.StringValue("cut"),
        )),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("stitched.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
        )),
    ),
    connections = listOf(
        ConnectionRef("import1", "video", "stitch", "videos"),
        ConnectionRef("import2", "video", "stitch", "videos"),
        ConnectionRef("stitch",  "video", "encode", "video"),
    ),
)
