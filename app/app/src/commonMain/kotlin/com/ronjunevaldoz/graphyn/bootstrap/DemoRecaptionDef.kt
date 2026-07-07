package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)

/**
 * Which TTS node type (and its own engine-specific config) to use for narration — one of
 * `media.text_to_speech.say` / `.qwen3` / `.oute` (see `MediaAiSpecs.kt` in `plugins/media-ai`).
 * [engine] must be one of those three suffixes; [params] are that node's config values.
 */
internal data class TtsEngineChoice(
    val engine: String,
    val params: Map<String, WorkflowValue>,
) {
    init {
        require(engine in setOf("say", "qwen3", "oute")) { "Unknown tts_engine '$engine'. Use say, qwen3, or oute." }
    }
}

/**
 * Restyles captions/narration on an already-stitched clip without redoing the Ollama + Flux
 * generation that produced it — reads back the `.stitched.mp4` and `.storyboard.json` that
 * [imageMotionStoryboardShortWorkflow] persists (see [STORYBOARD_OUTPUT_BASE]). [ttsEngine] lets
 * a later run swap the narration voice/engine (say/qwen3/oute) without touching Ollama/Flux at all.
 */
internal fun recaptionWorkflow(
    stitchedVideoPath: String,
    storyboardJsonPath: String,
    styleOverrides: Map<String, WorkflowValue> = CAPTION_STYLE_DEFAULTS,
    outputPath: String = "$STORYBOARD_OUTPUT_BASE.recaptioned.mp4",
    // "" (not a named speaker like "Ryan") — resolveQwen3VoiceRoute only treats a blank voice
    // as "use the model's default voice"; the Base/cloning model this project downloads has no
    // named speakers at all (only the CustomVoice variant does), so any non-blank value here
    // must be a real Qwen3Speaker name or it fails loudly.
    ttsEngine: TtsEngineChoice = TtsEngineChoice("qwen3", mapOf("voice" to s(""))),
) = WorkflowDefinition(
    id = "image-motion-storyboard-recaption",
    name = "Image Motion Short (Recaption)",
    nodes = listOf(
        NodeRef("videoIn", "media.video_import", config = mapOf("path" to s(stitchedVideoPath))),
        NodeRef("storyboardRead", "io.file_read", config = mapOf("path" to s(storyboardJsonPath))),
        NodeRef("storyboardParse", "json.parse"),
        NodeRef("narration", STORYBOARD_FIELD_NODE_TYPE, config = mapOf("field" to s("narration"))),
        NodeRef("captionsScript", STORYBOARD_CAPTIONS_NODE_TYPE, config = mapOf(
            "scene_duration_ms" to d(STORYBOARD_SCENE_DURATION_MS),
        )),
        NodeRef("captionStyle", CAPTION_STYLE_NODE_TYPE, config = CAPTION_STYLE_DEFAULTS + styleOverrides),
        NodeRef("captionOverlay", "media.caption_overlay"),
        NodeRef("narrate", "media.text_to_speech.${ttsEngine.engine}", config = ttsEngine.params),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to s(outputPath), "bitrate" to s("high"), "codec" to s("h264"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("storyboardRead", "content", "storyboardParse", "text"),
        ConnectionRef("storyboardParse", "value", "narration", "input"),
        ConnectionRef("storyboardParse", "value", "captionsScript", "input"),
        ConnectionRef("videoIn", "video", "captionOverlay", "video"),
        ConnectionRef("captionsScript", "result", "captionOverlay", "captions"),
        ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"),
        ConnectionRef("narration", "result", "narrate", "text"),
        ConnectionRef("captionOverlay", "video", "encode", "video"),
        ConnectionRef("narrate", "audio", "encode", "audio"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)
