package com.ronjunevaldoz.graphyn.plugins.mediacore.mapper

import com.ronjunevaldoz.graphyn.core.common.toColor
import com.ronjunevaldoz.graphyn.core.common.toHexColor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.booleanOr
import com.ronjunevaldoz.graphyn.core.model.booleanOrError
import com.ronjunevaldoz.graphyn.core.model.intOr
import com.ronjunevaldoz.graphyn.core.model.intOrError
import com.ronjunevaldoz.graphyn.core.model.stringOr
import com.ronjunevaldoz.graphyn.core.model.stringOrError
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionAlignment
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle

/**
 *  Should be used for Nodes
 */
fun Map<String, WorkflowValue>.toCaptionStyleWithDefaults(): CaptionStyle {
    return CaptionStyle(
        fontFamily = stringOr("font_family", "Arial"),
        fontSize = intOr("font_size", 42),
        textColor = stringOr("text_color", "#FFFFFF").toColor(),
        backgroundColor = stringOr("background_color", "#80000000").toColor(),
        outlineColor = stringOr("outline_color", "#000000").toColor(),
        outlineWidth = intOr("outline_width", 2),
        shadow = intOr("shadow", 0),
        bold = booleanOr("bold", true),
        italic = booleanOr("italic", false),
        alignment = CaptionAlignment.valueOf(
            stringOr("alignment", CaptionAlignment.BottomCenter.name),
        ),
        marginHorizontal = intOr("margin_horizontal", 40),
        marginVertical = intOr("margin_vertical", 60),
    )
}

/**
 * Should be used for executors
 */
fun Map<String, WorkflowValue>.toCaptionStyle(): CaptionStyle =
    CaptionStyle(
        fontFamily = stringOrError("font_family"),
        fontSize = intOrError("font_size"),
        textColor = stringOrError("text_color").toColor(),
        backgroundColor = stringOrError("background_color").toColor(),
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
            "background_color" to WorkflowValue.StringValue(
                backgroundColor?.toHexColor() ?: "#80000000",
            ),
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


