package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Composable
internal fun FieldHeader(label: String, theme: FieldNodeTheme) {
    Box(
        modifier = Modifier.fillMaxWidth().height(HEADER_DP.dp).background(theme.headerBackground())
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(label, style = TextStyle(color = theme.titleColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
internal fun FieldBody(inputs: List<PortSpec>, defaults: Map<String, WorkflowValue>, theme: FieldNodeTheme) {
    inputs.forEach { input ->
        InputRow(input = input, defaultVal = defaults[input.name]?.label(), theme = theme)
    }
}

@Composable
internal fun FieldFooter(outputs: List<PortSpec>, theme: FieldNodeTheme) {
    Box(Modifier.fillMaxWidth().height(FOOTER_DIVIDER_DP.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.divider()))
    }
    outputs.forEach { output -> OutputRow(output = output, theme = theme) }
}

@Composable
private fun InputRow(input: PortSpec, defaultVal: String?, theme: FieldNodeTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
        if (defaultVal != null) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.widthIn(min = 40.dp).clip(RoundedCornerShape(3.dp))
                    .background(theme.valueBg()).padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) { BasicText(defaultVal, style = TextStyle(color = theme.valueText(), fontSize = 10.sp)) }
        }
    }
}

@Composable
private fun OutputRow(output: PortSpec, theme: FieldNodeTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        BasicText(output.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
    }
}

internal fun WorkflowValue.label(): String = when (this) {
    is WorkflowValue.IntValue     -> value.toString()
    is WorkflowValue.DoubleValue  -> (kotlin.math.round(value * 1000) / 1000.0).toString()
    is WorkflowValue.StringValue  -> value
    is WorkflowValue.BooleanValue -> if (value) "true" else "false"
    else -> ""
}
