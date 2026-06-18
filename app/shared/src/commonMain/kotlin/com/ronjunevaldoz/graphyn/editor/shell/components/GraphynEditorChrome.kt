package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun GraphynSidePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = GraphynDs.colors
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .background(colors.panelBackground)
            .border(
                width = 1.dp,
                color = colors.border,
            ),
    ) {
        content()
    }
}

@Composable
internal fun GraphynTopBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = GraphynDs.colors
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .background(colors.panelBackground)
            .border(
                width = 1.dp,
                color = colors.border,
            ),
    ) {
        content()
    }
}
