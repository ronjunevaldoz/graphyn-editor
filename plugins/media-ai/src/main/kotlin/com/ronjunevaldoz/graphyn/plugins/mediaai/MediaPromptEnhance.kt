package com.ronjunevaldoz.graphyn.plugins.mediaai

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr

internal val promptEnhanceExecutor = NodeExecutor { inputs ->
    val prompt = buildShortPrompt(inputs)
    mapOf(
        "prompt" to WorkflowValue.StringValue(prompt),
        "negative_prompt" to WorkflowValue.StringValue(
            inputs.stringOr("negative_prompt", "blurry, low quality, cropped, watermark"),
        ),
    )
}

val promptEnhanceSpec = NodeSpec(
    type = "media.prompt_enhance",
    label = "Prompt Enhance",
    description = "Turns a rough scene brief into a richer generation prompt.",
    category = CATEGORY_MEDIA_AI,
    inputs = listOf(
        PortSpec("prompt", WorkflowType.OpaqueType),
        PortSpec("niche", WorkflowType.StringType),
        PortSpec("character", WorkflowType.StringType, description = "Reusable subject description, kept identical across scenes for visual continuity"),
        PortSpec("topic", WorkflowType.StringType),
        PortSpec("visual_style", WorkflowType.StringType),
        PortSpec("camera_move", WorkflowType.StringType),
        PortSpec("framing", WorkflowType.StringType),
        PortSpec("lighting", WorkflowType.StringType),
        PortSpec("details", WorkflowType.StringType),
        PortSpec("negative_prompt", WorkflowType.StringType),
    ),
    outputs = listOf(
        PortSpec("prompt", WorkflowType.StringType),
        PortSpec("negative_prompt", WorkflowType.StringType),
    ),
    defaultValues = mapOf(
        "niche" to WorkflowValue.StringValue(""),
        "character" to WorkflowValue.StringValue(""),
        "topic" to WorkflowValue.StringValue(""),
        "visual_style" to WorkflowValue.StringValue(""),
        "camera_move" to WorkflowValue.StringValue(""),
        "framing" to WorkflowValue.StringValue("vertical 9:16 composition"),
        "lighting" to WorkflowValue.StringValue("cinematic lighting"),
        "details" to WorkflowValue.StringValue("high detail, clean subject separation"),
        "negative_prompt" to WorkflowValue.StringValue("blurry, low quality, cropped, watermark"),
    ),
)

internal fun buildShortPrompt(inputs: Map<String, WorkflowValue>): String {
    val base = inputs.stringOr("prompt", "").trim()
    val parts = listOf(
        inputs.stringOr("niche", ""),
        inputs.stringOr("character", ""),
        inputs.stringOr("topic", ""),
        inputs.stringOr("visual_style", ""),
        inputs.stringOr("camera_move", ""),
        inputs.stringOr("framing", ""),
        inputs.stringOr("lighting", ""),
        inputs.stringOr("details", ""),
        "vertical, cinematic, highly detailed",
    ).filter(String::isNotBlank)
    return (listOf(base) + parts).filter(String::isNotBlank).joinToString(", ")
}
