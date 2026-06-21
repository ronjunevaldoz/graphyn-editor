package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Composable
internal fun InputRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    var editText by remember { mutableStateOf<String?>(null) }
    var focusGranted by remember { mutableStateOf(false) }
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
        BasicText(input.name, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
        if (currentValue != null) {
            Spacer(Modifier.weight(1f))
            if (input.type == WorkflowType.BooleanType && currentValue is WorkflowValue.BooleanValue) {
                val on = currentValue.value
                val activeBg = appTheme.colors.primary
                val activeText = appTheme.colors.onPrimary
                Box(
                    modifier = Modifier.width(VALUE_DP.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (on) activeBg else theme.valueBg())
                        .clickable { onValueChange(WorkflowValue.BooleanValue(!on)) }
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText(if (on) "ON" else "OFF", style = appTheme.typography.nodeLabel.copy(color = if (on) activeText else theme.valueText())) }
            } else if (editText != null) {
                BasicTextField(
                    value = editText!!,
                    onValueChange = { editText = it },
                    modifier = Modifier.width(VALUE_DP.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (it.isFocused) focusGranted = true else if (focusGranted) commit() },
                    textStyle = appTheme.typography.nodeLabel.copy(color = theme.valueText(), textAlign = TextAlign.Center),
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
                    modifier = Modifier.width(VALUE_DP.dp).clip(RoundedCornerShape(3.dp))
                        .background(theme.valueBg())
                        .clickable { focusGranted = false; editText = currentValue.label() }
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText(currentValue.label(), style = appTheme.typography.nodeLabel.copy(color = theme.valueText())) }
            }
        }
    }
}

@Composable
internal fun OutputRow(output: PortSpec, theme: FieldNodeTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        BasicText(output.name, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
    }
}

internal fun parseValue(original: WorkflowValue?, raw: String): WorkflowValue? = when (original) {
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
