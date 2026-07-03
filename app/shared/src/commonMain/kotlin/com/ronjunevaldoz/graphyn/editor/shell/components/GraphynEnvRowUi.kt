package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun ValueRow(row: EnvRow, onValue: (String) -> Unit, onKey: (String) -> Unit, onRemove: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val actionWidth = if (row.pinned) 0.dp else 28.dp
        val valueWidth = (maxWidth - 120.dp - actionWidth - 12.dp).coerceAtLeast(120.dp)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            CredInput(row.key, "key", Modifier.width(120.dp), onKey)
            CredInput(row.value, "value", Modifier.width(valueWidth), onValue)
            if (!row.pinned) MiniButton("✕") { onRemove() }
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
