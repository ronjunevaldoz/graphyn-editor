package com.ronjunevaldoz.graphyn.plugins.mediacore.mapper

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.intOrError
import com.ronjunevaldoz.graphyn.core.model.stringOrError
import com.ronjunevaldoz.graphyn.plugins.mediacore.ComparisonLayoutStyle

/** Should be used for executors. */
fun Map<String, WorkflowValue>.toComparisonLayoutStyle(): ComparisonLayoutStyle =
    ComparisonLayoutStyle(
        backgroundColor = stringOrError("background_color"),
        labelFontFamily = stringOrError("label_font_family"),
        labelFontSize = intOrError("label_font_size"),
        labelColor = stringOrError("label_color"),
        captionFontFamily = stringOrError("caption_font_family"),
        captionFontSize = intOrError("caption_font_size"),
        captionColor = stringOrError("caption_color"),
        panelGap = intOrError("panel_gap"),
    )
