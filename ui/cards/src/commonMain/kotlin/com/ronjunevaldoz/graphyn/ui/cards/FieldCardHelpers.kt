package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
internal fun FieldBody(
    inputs: List<PortSpec>,
    values: Map<String, WorkflowValue>,
    onValueChange: (key: String, value: WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    inputs.forEach { input ->
        InputRow(
            input = input,
            currentValue = values[input.name],
            onValueChange = { v -> onValueChange(input.name, v) },
            theme = theme,
        )
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
private fun InputRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    var editText by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    fun commit() {
        val raw = editText ?: return
        editText = null
        val parsed = parseValue(currentValue, raw) ?: return
        onValueChange(parsed)
    }

    LaunchedEffect(editText) { if (editText != null) focusRequester.requestFocus() }

    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
        if (currentValue != null) {
            Spacer(Modifier.weight(1f))
            if (editText != null) {
                BasicTextField(
                    value = editText!!,
                    onValueChange = { editText = it },
                    modifier = Modifier.widthIn(min = 40.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (!it.isFocused) commit() },
                    textStyle = TextStyle(color = theme.valueText(), fontSize = 10.sp, textAlign = TextAlign.Center),
                    decorationBox = { inner ->
                        Box(
                            Modifier.clip(RoundedCornerShape(3.dp)).background(theme.valueBg())
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) { inner() }
                    },
                )
            } else {
                Box(
                    modifier = Modifier.widthIn(min = 40.dp).clip(RoundedCornerShape(3.dp))
                        .background(theme.valueBg())
                        .clickable { editText = currentValue.label() }
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText(currentValue.label(), style = TextStyle(color = theme.valueText(), fontSize = 10.sp)) }
            }
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

private fun parseValue(original: WorkflowValue?, raw: String): WorkflowValue? = when (original) {
    is WorkflowValue.IntValue     -> raw.toIntOrNull()?.let { WorkflowValue.IntValue(it) }
    is WorkflowValue.DoubleValue  -> raw.toDoubleOrNull()?.let { WorkflowValue.DoubleValue(it) }
    is WorkflowValue.StringValue  -> WorkflowValue.StringValue(raw)
    is WorkflowValue.BooleanValue -> raw.toBooleanStrictOrNull()?.let { WorkflowValue.BooleanValue(it) }
    else -> null
}
internal fun WorkflowValue.label(): String = when (this) {
    is WorkflowValue.IntValue     -> value.toString()
    is WorkflowValue.DoubleValue  -> (kotlin.math.round(value * 1000) / 1000.0).toString()
    is WorkflowValue.StringValue  -> value
    is WorkflowValue.BooleanValue -> if (value) "true" else "false"
    else -> ""
}
