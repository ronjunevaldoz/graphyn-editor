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
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.GraphynSpacingValues
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
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
    FieldRow(name = input.name) {
        Box {
            Box(Modifier.width(VALUE_DP.dp).clip(RoundedCornerShape(GraphynSpacingValues.spacing.md)).background(theme.valueBg()).clickable { showPopup = true }.padding(horizontal = GraphynSpacingValues.spacing.xl, vertical = GraphynSpacingValues.spacing.xs), Alignment.Center) {
                BasicText("{ $label } ▾", style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
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
    Column(Modifier.widthIn(min = RECORD_POPUP_MIN_DP.dp, max = RECORD_POPUP_MAX_DP.dp).clip(RoundedCornerShape(appTheme.shapes.md)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(appTheme.shapes.md)).padding(vertical = GraphynSpacingValues.spacing.xs)) {
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
    var editText by remember { mutableStateOf<String?>(null) }
    var focusGranted by remember { mutableStateOf(false) }
    val display = value?.label() ?: ""
    fun commit() {
        val raw = editText ?: return; editText = null
        parseRecordField(type, raw)?.let(onEdit)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = GraphynSpacingValues.spacing.sm, vertical = GraphynSpacingValues.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        BasicText(key, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(GraphynSpacingValues.spacing.xxl))
        if (type == WorkflowType.BooleanType && value is WorkflowValue.BooleanValue) {
            val on = value.value
            val activeBg = appTheme.colors.primary
            val activeText = appTheme.colors.onPrimary
            Box(
                Modifier.widthIn(min = VALUE_MIN_DP.dp, max = VALUE_MAX_DP.dp).clip(RoundedCornerShape(appTheme.shapes.xs))
                    .background(if (on) activeBg else theme.valueBg())
                    .clickable { onEdit(WorkflowValue.BooleanValue(!on)) }
                    .padding(horizontal = appTheme.spacing.xs, vertical = appTheme.spacing.xxs),
            ) { BasicText(if (on) "ON" else "OFF", style = appTheme.typography.nodeLabel.copy(color = if (on) activeText else theme.valueText())) }
        } else if (editText != null) {
            BasicTextField(
                value = editText!!,
                onValueChange = { editText = it },
                modifier = Modifier.widthIn(min = VALUE_MIN_DP.dp, max = VALUE_MAX_DP.dp)
                    .onFocusChanged { if (it.isFocused) focusGranted = true else if (focusGranted) commit() },
                textStyle = appTheme.typography.nodeLabel.copy(color = theme.valueText()),
                decorationBox = { inner -> Box(Modifier.clip(RoundedCornerShape(appTheme.shapes.xs)).background(theme.valueBg()).padding(horizontal = appTheme.spacing.xs, vertical = appTheme.spacing.xxs)) { inner() } },
                singleLine = true,
            )
        } else {
            Box(Modifier.widthIn(min = VALUE_MIN_DP.dp, max = VALUE_MAX_DP.dp).clip(RoundedCornerShape(appTheme.shapes.xs)).background(theme.valueBg()).clickable { focusGranted = false; editText = display }.padding(horizontal = appTheme.spacing.xs, vertical = appTheme.spacing.xxs)) {
                BasicText(display.ifEmpty { "—" }, style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
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
