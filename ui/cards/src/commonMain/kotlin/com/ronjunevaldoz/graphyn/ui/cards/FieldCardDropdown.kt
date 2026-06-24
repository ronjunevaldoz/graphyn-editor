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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.GraphynSpacingValues
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Composable
internal fun SingleSelectRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    options: List<String>,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    var showMenu by remember { mutableStateOf(false) }
    val selected = (currentValue as? WorkflowValue.StringValue)?.value
    Row(Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = GraphynSpacingValues.spacing.xxxl), verticalAlignment = Alignment.CenterVertically) {
        BasicText(input.name, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
        Spacer(Modifier.weight(1f))
        Box {
            ValueChip(selected ?: "—", theme) { showMenu = true }
            if (showMenu) {
                Popup(alignment = Alignment.BottomStart, onDismissRequest = { showMenu = false }) {
                    DropdownMenu(options, theme) { showMenu = false; onValueChange(WorkflowValue.StringValue(it)) }
                }
            }
        }
    }
}

@Composable
internal fun MultiSelectRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    options: List<String>,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    var showMenu by remember { mutableStateOf(false) }
    val selectedSet = remember(currentValue) {
        (currentValue as? WorkflowValue.ListValue)
            ?.items?.filterIsInstance<WorkflowValue.StringValue>()
            ?.map { it.value }?.toSet() ?: emptySet()
    }
    val label = when (selectedSet.size) { 0 -> "—"; 1 -> selectedSet.first(); else -> "${selectedSet.size} selected" }
    Row(Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = GraphynSpacingValues.spacing.xxxl), verticalAlignment = Alignment.CenterVertically) {
        BasicText(input.name, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
        Spacer(Modifier.weight(1f))
        Box {
            ValueChip(label, theme) { showMenu = true }
            if (showMenu) {
                Popup(alignment = Alignment.BottomStart, onDismissRequest = { showMenu = false }) {
                    MultiDropdownMenu(options, selectedSet, theme) { toggled ->
                        val next = if (toggled in selectedSet) selectedSet - toggled else selectedSet + toggled
                        onValueChange(WorkflowValue.ListValue(next.map { WorkflowValue.StringValue(it) }))
                    }
                }
            }
        }
    }
}

@Composable
private fun ValueChip(label: String, theme: FieldNodeTheme, onClick: () -> Unit) =
    Box(Modifier.width(VALUE_DP.dp).clip(RoundedCornerShape(GraphynSpacingValues.spacing.md)).background(theme.valueBg()).clickable(onClick = onClick).padding(horizontal = GraphynSpacingValues.spacing.xl, vertical = GraphynSpacingValues.spacing.sm), Alignment.Center) {
        BasicText(label, style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
    }

@Composable
private fun DropdownMenu(options: List<String>, theme: FieldNodeTheme, onSelect: (String) -> Unit) {
    Column(Modifier.widthIn(min = GraphynSpacingValues.spacing.fieldWidth, max = GraphynSpacingValues.spacing.fieldMaxWidth).clip(RoundedCornerShape(GraphynSpacingValues.spacing.xxl)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(GraphynSpacingValues.spacing.xxl)).padding(vertical = GraphynSpacingValues.spacing.lg)) {
        options.forEach { option ->
            Box(Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(horizontal = GraphynSpacingValues.spacing.xxxl, vertical = GraphynSpacingValues.spacing.xl)) {
                BasicText(option, style = appTheme.typography.nodeLabel.copy(color = theme.valueText()), maxLines = 1)
            }
        }
    }
}

@Composable
private fun MultiDropdownMenu(options: List<String>, selected: Set<String>, theme: FieldNodeTheme, onToggle: (String) -> Unit) {
    Column(Modifier.widthIn(min = GraphynSpacingValues.spacing.fieldWidth, max = GraphynSpacingValues.spacing.fieldMaxWidth).clip(RoundedCornerShape(GraphynSpacingValues.spacing.xxl)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(GraphynSpacingValues.spacing.xxl)).padding(vertical = GraphynSpacingValues.spacing.lg)) {
        options.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { onToggle(option) }.padding(horizontal = GraphynSpacingValues.spacing.xxxl, vertical = GraphynSpacingValues.spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(GraphynSpacingValues.spacing.huge).clip(RoundedCornerShape(GraphynSpacingValues.spacing.sm)).background(if (option in selected) theme.selectedBorder() else theme.valueBg()).border(1.dp, theme.border(), RoundedCornerShape(GraphynSpacingValues.spacing.sm)))
                Spacer(Modifier.width(GraphynSpacingValues.spacing.xxl))
                BasicText(option, style = appTheme.typography.nodeLabel.copy(color = theme.valueText()), maxLines = 1)
            }
        }
    }
}
