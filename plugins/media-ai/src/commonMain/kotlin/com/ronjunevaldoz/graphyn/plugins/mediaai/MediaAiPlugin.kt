package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleOr
import com.ronjunevaldoz.graphyn.core.model.stringOrError
import com.ronjunevaldoz.graphyn.core.model.stringOr
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCompositionTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.mapper.toCaptionStyle
import com.ronjunevaldoz.graphyn.plugins.mediacore.mapper.toWorkflowValue

class MediaAiPlugin(
    private val textToSpeechEngine: TextToSpeechEngine = resolveTtsEngine(),
    private val ttsCacheEngine: TtsCacheEngine = createTtCacheEngine(),
    private val speechToTextEngine: SpeechToTextEngine = createSpeechToTextEngine(),
    private val ocrEngine: OcrEngine = resolveOcrEngine(),
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
            text = inputs.stringOrError("text"),
            language = inputs.stringOr("language", "en"),
            voiceId = inputs.stringOr("voice_id", "default"),
            speed = inputs.doubleOr("speed", 1.0),
        )
        val result = ttsCacheEngine.getOrCreate(request, textToSpeechEngine)
        mapOf(
            "audio" to MediaTypes.audioValue(result.metadata.path),
            "duration_ms" to WorkflowValue.DoubleValue(result.metadata.durationMs),
            "cached" to WorkflowValue.BooleanValue(result.cacheHit),
        )
    }

    /**
     * This is the standard execution with complete validation, mapper result in thin & maintainable codes
     */
    private val captionStyleExecutor = NodeExecutor { inputs ->
        val style = inputs.toCaptionStyle()
        require(style.fontFamily.isNotBlank()) { "Caption font_family must not be blank." }
        require(style.fontSize > 0) { "Caption font_size must be positive." }
        require(style.outlineWidth >= 0) { "Caption outline_width must be >= 0." }
        require(style.shadow >= 0) { "Caption shadow must be >= 0." }
        require(style.marginHorizontal >= 0) { "Caption margin_horizontal must be >= 0." }
        require(style.marginVertical >= 0) { "Caption margin_vertical must be >= 0." }
        mapOf(
            "style_config" to style.toWorkflowValue(),
        )
    }
}


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
