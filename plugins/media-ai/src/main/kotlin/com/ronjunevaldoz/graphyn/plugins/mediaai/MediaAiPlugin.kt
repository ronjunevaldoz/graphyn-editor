package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCompositionTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes

class MediaAiPlugin(
    private val textToSpeechEngine: TextToSpeechEngine = CommandTextToSpeechEngine(),
    private val ttsCache: TtsCache = TtsCache(),
    private val speechToTextEngine: SpeechToTextEngine = CommandSpeechToTextEngine(),
    private val ocrEngine: OcrEngine = CommandOcrEngine(),
) : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.media.ai",
        displayName = "Media AI",
        version = "0.5.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        MediaAiSpecs.all.forEach(registrar::registerNodeSpec)
        registrar.registerExecutor(MediaAiSpecs.textToSpeech.type, textToSpeechExecutor())
        registrar.registerExecutor(MediaAiSpecs.captionStyle.type, captionStyleExecutor)
        registrar.registerExecutor(MediaAiSpecs.speechToText.type, speechToTextExecutor())
        registrar.registerExecutor(MediaAiSpecs.ocr.type, ocrExecutor())
    }

    private fun speechToTextExecutor() = NodeExecutor { inputs ->
        val result = speechToTextEngine.transcribe(
            SpeechToTextRequest(
                audioPath = MediaTypes.path(inputs["audio"], "audio"),
                language = inputs.stringOr("language", "en"),
            ),
        )
        mapOf(
            "text" to WorkflowValue.StringValue(result.text),
            "confidence" to WorkflowValue.DoubleValue(result.confidence),
            "segments" to MediaCompositionTypes.captionList(
                result.segments.map { Triple(it.text, it.startMs, it.endMs) },
            ),
        )
    }

    private fun ocrExecutor() = NodeExecutor { inputs ->
        val result = ocrEngine.recognize(
            imagePath = MediaTypes.path(inputs["image"], "image"),
            language = inputs.stringOr("language", "en"),
        )
        mapOf(
            "text" to WorkflowValue.StringValue(result.text),
            "blocks" to WorkflowValue.ListValue(result.blocks.map { it.toRecordValue() }),
        )
    }

    private fun textToSpeechExecutor() = NodeExecutor { inputs ->
        val request = TextToSpeechRequest(
            text = inputs.string("text"),
            language = inputs.stringOr("language", "en"),
            voiceId = inputs.stringOr("voice_id", "default"),
            speed = inputs.doubleOr("speed", 1.0),
        )
        val result = ttsCache.getOrCreate(request, textToSpeechEngine)
        mapOf(
            "audio" to MediaTypes.audioValue(result.metadata.path),
            "duration_ms" to WorkflowValue.DoubleValue(result.metadata.durationMs),
            "cached" to WorkflowValue.BooleanValue(result.cacheHit),
        )
    }

    private val captionStyleExecutor = NodeExecutor { inputs ->
        val color = inputs.stringOr("color", "#FFFFFF")
        val backgroundColor = inputs.stringOr("background_color", "#000000")
        require(color.isHexColor()) { "Caption color must use #RRGGBB or #AARRGGBB." }
        require(backgroundColor.isHexColor()) { "Caption background_color must use #RRGGBB or #AARRGGBB." }
        val fontSize = inputs.intOr("font_size", 24)
        require(fontSize > 0) { "Caption font_size must be positive." }
        val position = inputs.stringOr("position", "bottom")
        require(position in setOf("top", "center", "bottom")) { "Unsupported caption position '$position'." }
        mapOf(
            "style_config" to WorkflowValue.RecordValue(
                mapOf(
                    "color" to WorkflowValue.StringValue(color),
                    "background_color" to WorkflowValue.StringValue(backgroundColor),
                    "font_size" to WorkflowValue.IntValue(fontSize),
                    "position" to WorkflowValue.StringValue(position),
                ),
            ),
        )
    }
}

private fun Map<String, WorkflowValue>.string(key: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value
        ?.takeIf(String::isNotBlank)
        ?: error("Missing required string input '$key'.")

private fun Map<String, WorkflowValue>.stringOr(key: String, default: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value ?: default

private fun Map<String, WorkflowValue>.doubleOr(key: String, default: Double): Double = when (val value = this[key]) {
    is WorkflowValue.DoubleValue -> value.value
    is WorkflowValue.IntValue -> value.value.toDouble()
    else -> default
}

private fun Map<String, WorkflowValue>.intOr(key: String, default: Int): Int =
    (this[key] as? WorkflowValue.IntValue)?.value ?: default

private fun String.isHexColor(): Boolean =
    (length == 7 || length == 9) && startsWith("#") && drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

private fun OcrBlock.toRecordValue(): WorkflowValue.RecordValue = WorkflowValue.RecordValue(
    mapOf(
        "text" to WorkflowValue.StringValue(text),
        "x" to WorkflowValue.IntValue(x),
        "y" to WorkflowValue.IntValue(y),
        "width" to WorkflowValue.IntValue(width),
        "height" to WorkflowValue.IntValue(height),
        "confidence" to WorkflowValue.DoubleValue(confidence),
    ),
)
