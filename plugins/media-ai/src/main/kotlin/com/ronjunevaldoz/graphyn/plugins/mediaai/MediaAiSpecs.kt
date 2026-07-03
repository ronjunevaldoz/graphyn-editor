package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCompositionTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionAlignment

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
            "font_family" to WorkflowType.StringType,
            "font_size" to WorkflowType.IntType,
            "text_color" to WorkflowType.StringType,
            "background_color" to WorkflowType.NullableType(WorkflowType.StringType),
            "outline_color" to WorkflowType.StringType,
            "outline_width" to WorkflowType.IntType,
            "shadow" to WorkflowType.IntType,
            "bold" to WorkflowType.BooleanType,
            "italic" to WorkflowType.BooleanType,
            "alignment" to WorkflowType.EnumType(
                CaptionAlignment.entries.map { it.name },
            ),
            "margin_horizontal" to WorkflowType.IntType,
            "margin_vertical" to WorkflowType.IntType,
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
            PortSpec("font_family", WorkflowType.StringType),
            PortSpec("font_size", WorkflowType.IntType),
            PortSpec("text_color", WorkflowType.StringType),
            PortSpec("background_color", WorkflowType.NullableType(WorkflowType.StringType)),
            PortSpec("outline_color", WorkflowType.StringType),
            PortSpec("outline_width", WorkflowType.IntType),
            PortSpec("shadow", WorkflowType.IntType),
            PortSpec("bold", WorkflowType.BooleanType),
            PortSpec("italic", WorkflowType.BooleanType),
            PortSpec(
                "alignment",
                WorkflowType.EnumType(
                    CaptionAlignment.entries.map { it.name },
                ),
            ),
            PortSpec("margin_horizontal", WorkflowType.IntType),
            PortSpec("margin_vertical", WorkflowType.IntType),
        ),
        outputs = listOf(
            PortSpec("style_config", captionStyleType),
        ),
        defaultValues = mapOf(
            "font_family" to WorkflowValue.StringValue("Arial"),
            "font_size" to WorkflowValue.IntValue(42),
            "text_color" to WorkflowValue.StringValue("#FFFFFF"),
            "background_color" to WorkflowValue.NullValue,
            "outline_color" to WorkflowValue.StringValue("#000000"),
            "outline_width" to WorkflowValue.IntValue(2),
            "shadow" to WorkflowValue.IntValue(0),
            "bold" to WorkflowValue.BooleanValue(true),
            "italic" to WorkflowValue.BooleanValue(false),
            "alignment" to WorkflowValue.StringValue(CaptionAlignment.BottomCenter.name),
            "margin_horizontal" to WorkflowValue.IntValue(40),
            "margin_vertical" to WorkflowValue.IntValue(60),
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
