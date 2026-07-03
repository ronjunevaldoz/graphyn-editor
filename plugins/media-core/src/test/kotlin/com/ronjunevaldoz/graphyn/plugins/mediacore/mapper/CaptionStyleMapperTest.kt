package com.ronjunevaldoz.graphyn.plugins.mediacore.mapper

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionAlignment
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaptionStyleMapperTest {

    @Test
    fun nullBackgroundColorSurvivesWorkflowRoundTrip() {
        val record = sampleStyle(backgroundColor = null).toWorkflowValue().fields

        assertEquals(WorkflowValue.NullValue, record["background_color"])
        assertNull(record.toCaptionStyle().backgroundColor)
    }

    @Test
    fun blankBackgroundColorIsTreatedAsNull() {
        val style = baseInputs(WorkflowValue.StringValue("")).toCaptionStyle()

        assertNull(style.backgroundColor)
    }

    private fun sampleStyle(backgroundColor: androidx.compose.ui.graphics.Color?) =
        CaptionStyle(
            fontFamily = "Arial",
            fontSize = 42,
            textColor = androidx.compose.ui.graphics.Color.White,
            backgroundColor = backgroundColor,
            outlineColor = androidx.compose.ui.graphics.Color.Black,
            outlineWidth = 2,
            shadow = 0,
            bold = true,
            italic = false,
            alignment = CaptionAlignment.BottomCenter,
            marginHorizontal = 40,
            marginVertical = 60,
        )

    private fun baseInputs(backgroundColor: WorkflowValue) = mapOf(
        "font_family" to WorkflowValue.StringValue("Arial"),
        "font_size" to WorkflowValue.IntValue(42),
        "text_color" to WorkflowValue.StringValue("#FFFFFF"),
        "background_color" to backgroundColor,
        "outline_color" to WorkflowValue.StringValue("#000000"),
        "outline_width" to WorkflowValue.IntValue(2),
        "shadow" to WorkflowValue.IntValue(0),
        "bold" to WorkflowValue.BooleanValue(true),
        "italic" to WorkflowValue.BooleanValue(false),
        "alignment" to WorkflowValue.StringValue(CaptionAlignment.BottomCenter.name),
        "margin_horizontal" to WorkflowValue.IntValue(40),
        "margin_vertical" to WorkflowValue.IntValue(60),
    )
}
