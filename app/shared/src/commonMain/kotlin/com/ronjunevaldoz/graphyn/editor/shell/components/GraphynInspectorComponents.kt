package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun InspectorCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = GraphynDs.colors
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceCard)
            .border(1.dp, colors.border, shape)
            .padding(12.dp),
    ) {
        content()
    }
}

@Composable
internal fun InspectorSectionLabel(text: String) {
    BasicText(
        text = text,
        style = GraphynDs.type.labelSmall.copy(color = GraphynDs.colors.textDisabled),
    )
}

@Composable
internal fun DangerButton(label: String, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, colors.danger.copy(alpha = 0.5f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = GraphynDs.type.label.copy(color = colors.danger))
    }
}
