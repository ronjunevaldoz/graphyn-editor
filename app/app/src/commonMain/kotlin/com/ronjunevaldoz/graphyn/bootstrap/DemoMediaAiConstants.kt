package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// media.caption_style / media.prompt_enhance are JVM-only (plugins/media-ai shells out to
// platform TTS/STT/OCR CLIs) — type strings and default values are copied here so this KMP
// module stays dependency-free, matching the "script.eval is JVM-only" pattern in
// WorkflowCatalog.kt. Keep in sync with MediaAiSpecs.captionStyle / promptEnhanceSpec.
internal const val CAPTION_STYLE_NODE_TYPE = "media.caption_style"
internal const val PROMPT_ENHANCE_NODE_TYPE = "media.prompt_enhance"

internal val CAPTION_STYLE_DEFAULTS: Map<String, WorkflowValue> = mapOf(
    "font_family" to WorkflowValue.StringValue("Arial"),
    "font_size" to WorkflowValue.IntValue(42),
    "text_color" to WorkflowValue.StringValue("#FFFFFF"),
    "background_color" to WorkflowValue.NullValue,
    "outline_color" to WorkflowValue.StringValue("#000000"),
    "outline_width" to WorkflowValue.IntValue(2),
    "shadow" to WorkflowValue.IntValue(0),
    "bold" to WorkflowValue.BooleanValue(true),
    "italic" to WorkflowValue.BooleanValue(false),
    "alignment" to WorkflowValue.StringValue("BottomCenter"),
    "margin_horizontal" to WorkflowValue.IntValue(40),
    "margin_vertical" to WorkflowValue.IntValue(60),
)
