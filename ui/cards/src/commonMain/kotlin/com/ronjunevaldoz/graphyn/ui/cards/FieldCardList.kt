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
import androidx.compose.runtime.key
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
internal fun ListRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    elementType: WorkflowType,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    var showPopup by remember { mutableStateOf(false) }
    val items = (currentValue as? WorkflowValue.ListValue)?.items ?: emptyList()
    val label = when (items.size) { 0 -> "empty"; 1 -> "1 item"; else -> "${items.size} items" }
    Row(Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = GraphynSpacingValues.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        BasicText(input.name, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
        Spacer(Modifier.weight(1f))
        Box {
            Box(Modifier.width(VALUE_DP.dp).clip(RoundedCornerShape(GraphynSpacingValues.spacing.md)).background(theme.valueBg()).clickable { showPopup = true }.padding(horizontal = GraphynSpacingValues.spacing.xl, vertical = GraphynSpacingValues.spacing.xs), Alignment.Center) {
                BasicText("$label ▾", style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
            }
            if (showPopup) Popup(alignment = Alignment.BottomStart, onDismissRequest = { showPopup = false }) {
                ListPopup(items, elementType, theme) { onValueChange(WorkflowValue.ListValue(it)) }
            }
        }
    }
}

@Composable
private fun ListPopup(items: List<WorkflowValue>, elementType: WorkflowType, theme: FieldNodeTheme, onChange: (List<WorkflowValue>) -> Unit) {
    Column(Modifier.widthIn(min = LIST_POPUP_MIN_DP.dp, max = LIST_POPUP_MAX_DP.dp).clip(RoundedCornerShape(appTheme.shapes.md)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(appTheme.shapes.md)).padding(vertical = GraphynSpacingValues.spacing.xs)) {
        items.forEachIndexed { i, item ->
            key(i) {
                ListItemRow(item, elementType, theme,
                    onEdit = { updated -> onChange(items.toMutableList().also { it[i] = updated }) },
                    onRemove = { onChange(items.toMutableList().also { it.removeAt(i) }) },
                )
            }
        }
        Box(Modifier.fillMaxWidth().clickable { onChange(items + defaultItem(elementType)) }.padding(horizontal = GraphynSpacingValues.spacing.sm, vertical = GraphynSpacingValues.spacing.xl)) {
            BasicText("+ Add", style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
        }
    }
}

@Composable
private fun ListItemRow(value: WorkflowValue, elementType: WorkflowType, theme: FieldNodeTheme, onEdit: (WorkflowValue) -> Unit, onRemove: () -> Unit) {
    var editText by remember { mutableStateOf<String?>(null) }
    var focusGranted by remember { mutableStateOf(false) }
    fun commit() {
        val raw = editText ?: return; editText = null
        parseListItem(elementType, raw)?.let(onEdit)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = GraphynSpacingValues.spacing.xxl, vertical = GraphynSpacingValues.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        if (elementType == WorkflowType.BooleanType && value is WorkflowValue.BooleanValue) {
            val on = value.value
            val activeBg = appTheme.colors.primary
            val activeText = appTheme.colors.onPrimary
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(appTheme.shapes.xs))
                    .background(if (on) activeBg else theme.valueBg())
                    .clickable { onEdit(WorkflowValue.BooleanValue(!on)) }
                    .padding(horizontal = appTheme.spacing.xs, vertical = appTheme.spacing.xxs),
            ) { BasicText(if (on) "ON" else "OFF", style = appTheme.typography.nodeLabel.copy(color = if (on) activeText else theme.valueText())) }
        } else if (editText != null) {
            BasicTextField(
                value = editText!!,
                onValueChange = { editText = it },
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) focusGranted = true else if (focusGranted) commit() },
                textStyle = appTheme.typography.nodeLabel.copy(color = theme.valueText()),
                decorationBox = { inner -> Box(Modifier.clip(RoundedCornerShape(appTheme.shapes.xs)).background(theme.valueBg()).padding(appTheme.spacing.xxs)) { inner() } },
                singleLine = true,
            )
        } else {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(appTheme.shapes.xs)).background(theme.valueBg()).clickable { focusGranted = false; editText = value.label() }.padding(horizontal = appTheme.spacing.xs, vertical = appTheme.spacing.xxs)) {
                BasicText(value.label(), style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
            }
        }
        Spacer(Modifier.width(GraphynSpacingValues.spacing.xs))
        Box(Modifier.clickable(onClick = onRemove).padding(appTheme.spacing.xxs)) {
            BasicText("×", style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
        }
    }
}

private fun defaultItem(elementType: WorkflowType): WorkflowValue = when (elementType) {
    WorkflowType.IntType -> WorkflowValue.IntValue(0)
    WorkflowType.DoubleType -> WorkflowValue.DoubleValue(0.0)
    WorkflowType.BooleanType -> WorkflowValue.BooleanValue(false)
    else -> WorkflowValue.StringValue("")
}

private fun parseListItem(elementType: WorkflowType, raw: String): WorkflowValue? = when (elementType) {
    WorkflowType.IntType -> raw.toIntOrNull()?.let { WorkflowValue.IntValue(it) }
    WorkflowType.DoubleType -> raw.toDoubleOrNull()?.let { WorkflowValue.DoubleValue(it) }
    WorkflowType.BooleanType -> raw.toBooleanStrictOrNull()?.let { WorkflowValue.BooleanValue(it) }
    else -> WorkflowValue.StringValue(raw)
}
