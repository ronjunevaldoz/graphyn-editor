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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
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
    Row(Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
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
    Row(Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
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
    Box(Modifier.widthIn(min = 40.dp).clip(RoundedCornerShape(3.dp)).background(theme.valueBg()).clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 2.dp), Alignment.Center) {
        BasicText(label, style = TextStyle(color = theme.valueText(), fontSize = 10.sp))
    }

@Composable
private fun DropdownMenu(options: List<String>, theme: FieldNodeTheme, onSelect: (String) -> Unit) {
    Column(Modifier.widthIn(min = 80.dp).clip(RoundedCornerShape(6.dp)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(6.dp)).padding(vertical = 4.dp)) {
        options.forEach { option ->
            Box(Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                BasicText(option, style = TextStyle(color = theme.valueText(), fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun MultiDropdownMenu(options: List<String>, selected: Set<String>, theme: FieldNodeTheme, onToggle: (String) -> Unit) {
    Column(Modifier.widthIn(min = 80.dp).clip(RoundedCornerShape(6.dp)).background(theme.background()).border(1.dp, theme.border(), RoundedCornerShape(6.dp)).padding(vertical = 4.dp)) {
        options.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { onToggle(option) }.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (option in selected) theme.selectedBorder() else theme.valueBg()).border(1.dp, theme.border(), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(6.dp))
                BasicText(option, style = TextStyle(color = theme.valueText(), fontSize = 10.sp))
            }
        }
    }
}
