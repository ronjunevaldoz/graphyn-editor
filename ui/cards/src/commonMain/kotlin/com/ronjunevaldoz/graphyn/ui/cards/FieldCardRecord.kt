package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Composable
internal fun RecordRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    fieldTypes: Map<String, WorkflowType>,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    var showPopup by remember { mutableStateOf(false) }
    val fields = (currentValue as? WorkflowValue.RecordValue)?.fields ?: emptyMap()
    val label = when (fieldTypes.size) { 1 -> "1 field"; else -> "${fieldTypes.size} fields" }
    Row(Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
        Spacer(Modifier.weight(1f))
        Box {
            Box(Modifier.width(VALUE_DP.dp).clip(RoundedCornerShape(3.dp)).background(theme.valueBg()).clickable { showPopup = true }.padding(horizontal = 5.dp, vertical = 2.dp), Alignment.Center) {
                BasicText("{ $label } ▾", style = TextStyle(color = theme.valueText(), fontSize = 10.sp))
            }
            if (showPopup) Popup(alignment = Alignment.BottomStart, onDismissRequest = { showPopup = false }) {
                RecordPopup(fields, fieldTypes, theme) { onValueChange(WorkflowValue.RecordValue(it)) }
            }
        }
    }
}

@Composable
private fun RecordPopup(
    fields: Map<String, WorkflowValue>,
    fieldTypes: Map<String, WorkflowType>,
    theme: FieldNodeTheme,
    onChange: (Map<String, WorkflowValue>) -> Unit,
) {
    Column(Modifier.widthIn(min = 140.dp, max = 220.dp).clip(RoundedCornerShape(6.dp)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(6.dp)).padding(vertical = 4.dp)) {
        fieldTypes.forEach { (key, type) ->
            RecordFieldRow(
                key = key,
                value = fields[key],
                type = type,
                theme = theme,
                onEdit = { updated -> onChange(fields + (key to updated)) },
            )
        }
    }
}

@Composable
private fun RecordFieldRow(
    key: String,
    value: WorkflowValue?,
    type: WorkflowType,
    theme: FieldNodeTheme,
    onEdit: (WorkflowValue) -> Unit,
) {
    var editText by remember(value) { mutableStateOf<String?>(null) }
    var focusGranted by remember { mutableStateOf(false) }
    val display = value?.label() ?: ""
    fun commit() {
        val raw = editText ?: return; editText = null
        parseRecordField(type, raw)?.let(onEdit)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(key, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        if (editText != null) {
            BasicTextField(
                value = editText!!,
                onValueChange = { editText = it },
                modifier = Modifier.widthIn(min = 48.dp, max = 80.dp)
                    .onFocusChanged { if (it.isFocused) focusGranted = true else if (focusGranted) commit() },
                textStyle = TextStyle(color = theme.valueText(), fontSize = 10.sp),
                decorationBox = { inner -> Box(Modifier.clip(RoundedCornerShape(2.dp)).background(theme.valueBg()).padding(horizontal = 4.dp, vertical = 2.dp)) { inner() } },
                singleLine = true,
            )
        } else {
            Box(Modifier.widthIn(min = 48.dp, max = 80.dp).clip(RoundedCornerShape(2.dp)).background(theme.valueBg()).clickable { focusGranted = false; editText = display }.padding(horizontal = 4.dp, vertical = 2.dp)) {
                BasicText(display.ifEmpty { "—" }, style = TextStyle(color = theme.valueText(), fontSize = 10.sp))
            }
        }
    }
}

private fun parseRecordField(type: WorkflowType, raw: String): WorkflowValue? = when (type) {
    WorkflowType.IntType -> raw.toIntOrNull()?.let { WorkflowValue.IntValue(it) }
    WorkflowType.DoubleType -> raw.toDoubleOrNull()?.let { WorkflowValue.DoubleValue(it) }
    WorkflowType.BooleanType -> raw.toBooleanStrictOrNull()?.let { WorkflowValue.BooleanValue(it) }
    else -> WorkflowValue.StringValue(raw)
}
