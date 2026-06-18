package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun PaletteNodeItem(spec: NodeSpec, onAdd: (NodeSpec) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null) { onAdd(spec) }
            .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(colors.textDisabled),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(text = spec.label, style = type.body.copy(color = colors.textPrimary))
            spec.description?.let { BasicText(text = it, style = type.bodySmall.copy(color = colors.textDisabled)) }
        }
    }
}

@Composable
internal fun PaletteCategoryHeader(
    meta: NodeCategoryMeta,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val accent = Color(meta.color)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        BasicText(
            text = meta.label,
            style = type.label.copy(color = colors.textSecondary),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = if (expanded) "▾" else "▸",
            style = type.bodySmall.copy(color = colors.textDisabled),
        )
    }
}
