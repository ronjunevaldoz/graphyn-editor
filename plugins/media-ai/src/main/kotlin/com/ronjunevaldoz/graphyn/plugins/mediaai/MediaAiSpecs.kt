package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCompositionTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes

const val CATEGORY_MEDIA_AI = "graphyn.media.ai"

private val STT_LANGUAGES = listOf("en", "zh", "es", "fr", "de", "ja", "ko")

object MediaAiSpecs {
    val ocrBlockType = WorkflowType.RecordType(
        mapOf(
            "text" to WorkflowType.StringType,
            "x" to WorkflowType.IntType,
            "y" to WorkflowType.IntType,
            "width" to WorkflowType.IntType,
            "height" to WorkflowType.IntType,
            "confidence" to WorkflowType.DoubleType,
        ),
    )

    val captionStyleType = WorkflowType.RecordType(
        mapOf(
            "color" to WorkflowType.StringType,
            "background_color" to WorkflowType.StringType,
            "font_size" to WorkflowType.IntType,
            "position" to WorkflowType.EnumType(listOf("top", "center", "bottom")),
        ),
    )

    val textToSpeech = NodeSpec(
        type = "media.text_to_speech",
        label = "Text to Speech",
        description = "Generates speech using the configured GRAPHYN_TTS_EXECUTABLE adapter.",
        category = CATEGORY_MEDIA_AI,
        inputs = listOf(
            PortSpec("text", WorkflowType.StringType),
            PortSpec(
                "language",
                WorkflowType.EnumType(listOf("en", "zh", "es", "fr", "de", "ja", "ko")),
            ),
            PortSpec("voice_id", WorkflowType.StringType),
            PortSpec("speed", WorkflowType.DoubleType),
        ),
        outputs = listOf(
            PortSpec("audio", MediaTypes.audioHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
            PortSpec("cached", WorkflowType.BooleanType),
        ),
        defaultValues = mapOf(
            "text" to WorkflowValue.StringValue(""),
            "language" to WorkflowValue.StringValue("en"),
            "voice_id" to WorkflowValue.StringValue("default"),
            "speed" to WorkflowValue.DoubleValue(1.0),
        ),
    )

    val captionStyle = NodeSpec(
        type = "media.caption_style",
        label = "Caption Style",
        description = "Creates reusable caption appearance metadata.",
        category = CATEGORY_MEDIA_AI,
        inputs = listOf(
            PortSpec("color", WorkflowType.StringType),
            PortSpec("background_color", WorkflowType.StringType),
            PortSpec("font_size", WorkflowType.IntType),
            PortSpec(
                "position",
                WorkflowType.EnumType(listOf("top", "center", "bottom")),
            ),
        ),
        outputs = listOf(PortSpec("style_config", captionStyleType)),
        defaultValues = mapOf(
            "color" to WorkflowValue.StringValue("#FFFFFF"),
            "background_color" to WorkflowValue.StringValue("#000000"),
            "font_size" to WorkflowValue.IntValue(24),
            "position" to WorkflowValue.StringValue("bottom"),
        ),
    )

    val speechToText = NodeSpec(
        type = "media.speech_to_text",
        label = "Speech to Text",
        description = "Transcribes audio into text plus timed caption segments via GRAPHYN_STT_EXECUTABLE.",
        category = CATEGORY_MEDIA_AI,
        inputs = listOf(
            PortSpec("audio", MediaTypes.audioHandle),
            PortSpec("language", WorkflowType.EnumType(STT_LANGUAGES)),
        ),
        outputs = listOf(
            PortSpec("text", WorkflowType.StringType),
            PortSpec("confidence", WorkflowType.DoubleType),
            PortSpec("segments", MediaCompositionTypes.captions),
        ),
        defaultValues = mapOf("language" to WorkflowValue.StringValue("en")),
    )

    val ocr = NodeSpec(
        type = "media.ocr",
        label = "OCR",
        description = "Extracts text and bounding blocks from an image via GRAPHYN_OCR_EXECUTABLE.",
        category = CATEGORY_MEDIA_AI,
        inputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("language", WorkflowType.EnumType(STT_LANGUAGES)),
        ),
        outputs = listOf(
            PortSpec("text", WorkflowType.StringType),
            PortSpec("blocks", WorkflowType.ListType(ocrBlockType)),
        ),
        defaultValues = mapOf("language" to WorkflowValue.StringValue("en")),
    )

    val all = listOf(textToSpeech, captionStyle, speechToText, ocr)
}
