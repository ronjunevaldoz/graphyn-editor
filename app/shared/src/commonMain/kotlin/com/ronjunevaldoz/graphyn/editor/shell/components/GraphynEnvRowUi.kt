package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun ValueRow(row: EnvRow, onValue: (String) -> Unit, onKey: (String) -> Unit, onRemove: () -> Unit) {
    var colorPickerOpen by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val actionWidth = if (row.pinned) 0.dp else 28.dp
        val pickerWidth = if (supportsColorPicker(row)) 30.dp else 0.dp
        val valueWidth = (maxWidth - 120.dp - actionWidth - pickerWidth - 18.dp).coerceAtLeast(120.dp)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            CredInput(row.key, "key", Modifier.width(120.dp), onKey)
            CredInput(row.value, "value", Modifier.width(valueWidth), onValue)
            if (supportsColorPicker(row)) {
                ColorSwatchButton(value = row.value, onToggle = { colorPickerOpen = !colorPickerOpen })
            }
            if (!row.pinned) MiniButton("✕") { onRemove() }
        }
        if (colorPickerOpen && supportsColorPicker(row)) {
            ColorPickerPalette(
                modifier = Modifier.padding(start = 126.dp, top = 38.dp),
                onPick = { color -> onValue(color); colorPickerOpen = false },
            )
        }
    }
}

@Composable
internal fun CredInput(value: String, hint: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Box(modifier.heightIn(min = 32.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, colors.border, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) {
        if (value.isEmpty()) BasicText(hint, style = type.bodySmall.copy(color = colors.textDisabled))
        BasicTextField(value, onChange, textStyle = type.bodySmall.copy(color = colors.textPrimary), cursorBrush = SolidColor(colors.accent), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ColorSwatchButton(value: String, onToggle: () -> Unit) {
    val color = value.toHexColorOrNull() ?: Color(0xFF9B9BFF)
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color).border(1.dp, GraphynDs.colors.border, RoundedCornerShape(6.dp))
            .clickableNoRipple(onToggle),
    ) {
    }
}

@Composable
private fun ColorPickerPalette(modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    val swatches = listOf("#F55A4A", "#2F6BFF", "#7AA2FF", "#4CAF50", "#F59E0B", "#A855F7", "#FFFFFF", "#111827")
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(GraphynDs.colors.panelBackground).border(1.dp, GraphynDs.colors.border, RoundedCornerShape(8.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { hex ->
                    val color = hex.toHexColorOrNull() ?: Color.Transparent
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(color).border(1.dp, GraphynDs.colors.borderSubtle, RoundedCornerShape(5.dp)).clickableNoRipple { onPick(hex) })
                }
            }
        }
    }
}

private fun supportsColorPicker(row: EnvRow): Boolean = row.key.contains("color", ignoreCase = true) || row.value.toHexColorOrNull() != null

private fun String.toHexColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    val parsed = hex.toLongOrNull(16) ?: return null
    val argb = if (hex.length == 6) (0xFF000000 or parsed) else parsed
    return Color(argb.toInt())
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    androidx.compose.foundation.clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
