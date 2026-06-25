package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun PaletteNodeItem(spec: NodeSpec, indent: Dp = 0.dp, onAdd: (NodeSpec) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null) { onAdd(spec) }
            .padding(start = 24.dp + indent, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(colors.textDisabled),
        )
        BasicText(text = spec.label, style = type.body.copy(color = colors.textPrimary))
    }
}

/** A collapsible parent folder grouping several categories (e.g. "Socials"). */
@Composable
internal fun PaletteFolderHeader(label: String, expanded: Boolean, onToggle: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(text = if (expanded) "▾" else "▸", style = type.bodySmall.copy(color = colors.textDisabled))
        BasicText(
            text = label,
            style = type.panelTitle.copy(color = colors.textSecondary),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun PaletteCategoryHeader(
    meta: NodeCategoryMeta,
    expanded: Boolean,
    indent: Dp = 0.dp,
    onToggle: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(start = 12.dp + indent, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Neutral marker — category identity comes from the label, not a per-brand colour.
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.textDisabled),
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
