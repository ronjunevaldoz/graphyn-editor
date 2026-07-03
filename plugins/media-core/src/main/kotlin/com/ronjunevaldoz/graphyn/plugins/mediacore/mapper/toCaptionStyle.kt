package com.ronjunevaldoz.graphyn.plugins.mediacore.mapper

import com.ronjunevaldoz.graphyn.core.common.toColor
import com.ronjunevaldoz.graphyn.core.common.toHexColor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.booleanOrError
import com.ronjunevaldoz.graphyn.core.model.intOrError
import com.ronjunevaldoz.graphyn.core.model.stringOrError
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionAlignment
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle

/**
 * Should be used for executors
 */
fun Map<String, WorkflowValue>.toCaptionStyle(): CaptionStyle =
    CaptionStyle(
        fontFamily = stringOrError("font_family"),
        fontSize = intOrError("font_size"),
        textColor = stringOrError("text_color").toColor(),
        backgroundColor = when (val raw = get("background_color")) {
            null,
            WorkflowValue.NullValue -> null
            is WorkflowValue.StringValue -> raw.value.takeIf(String::isNotBlank)?.toColor()
            else -> error("Expected string field 'background_color'.")
        },
        outlineColor = stringOrError("outline_color").toColor(),
        outlineWidth = intOrError("outline_width"),
        shadow = intOrError("shadow"),
        bold = booleanOrError("bold"),
        italic = booleanOrError("italic"),
        alignment = CaptionAlignment.valueOf(stringOrError("alignment")),
        marginHorizontal = intOrError("margin_horizontal"),
        marginVertical = intOrError("margin_vertical"),
    )

fun CaptionStyle.toWorkflowValue(): WorkflowValue.RecordValue =
    WorkflowValue.RecordValue(
        mapOf(
            "font_family" to WorkflowValue.StringValue(fontFamily),
            "font_size" to WorkflowValue.IntValue(fontSize),
            "text_color" to WorkflowValue.StringValue(textColor.toHexColor()),
            "background_color" to (backgroundColor?.let { WorkflowValue.StringValue(it.toHexColor()) }
                ?: WorkflowValue.NullValue),
            "outline_color" to WorkflowValue.StringValue(outlineColor.toHexColor()),
            "outline_width" to WorkflowValue.IntValue(outlineWidth),
            "shadow" to WorkflowValue.IntValue(shadow),
            "bold" to WorkflowValue.BooleanValue(bold),
            "italic" to WorkflowValue.BooleanValue(italic),
            "alignment" to WorkflowValue.StringValue(alignment.name),
            "margin_horizontal" to WorkflowValue.IntValue(marginHorizontal),
            "margin_vertical" to WorkflowValue.IntValue(marginVertical),
        ),
    )

