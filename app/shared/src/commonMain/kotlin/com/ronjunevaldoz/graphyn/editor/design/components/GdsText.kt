package com.ronjunevaldoz.graphyn.editor.design.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
fun GdsText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val ds = GraphynDs
    val resolved = (style ?: ds.type.body).let { s ->
        if (color != null) s.copy(color = color) else s.copy(color = ds.colors.textPrimary)
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun GdsPanelTitle(text: String, modifier: Modifier = Modifier) {
    val ds = GraphynDs
    BasicText(
        text = text.uppercase(),
        modifier = modifier,
        style = ds.type.panelTitle.copy(color = ds.colors.textSecondary),
    )
}

@Composable
fun GdsNodeTitle(text: String, modifier: Modifier = Modifier) {
    val ds = GraphynDs
    BasicText(
        text = text,
        modifier = modifier,
        style = ds.type.nodeTitle.copy(color = ds.colors.textPrimary),
    )
}

@Composable
fun GdsLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val ds = GraphynDs
    BasicText(
        text = text,
        modifier = modifier,
        style = ds.type.label.copy(color = color ?: ds.colors.textSecondary),
    )
}
