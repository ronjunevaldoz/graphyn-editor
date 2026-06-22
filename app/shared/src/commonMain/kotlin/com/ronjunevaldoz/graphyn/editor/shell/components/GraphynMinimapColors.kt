package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

internal data class MiniMapColors(
    val background: Color,
    val emptyStroke: Color,
    val nodeFill: Color,
    val nodeStroke: Color,
    val viewportFill: Color,
    val viewportStroke: Color,
)

@Composable
internal fun rememberMinimapColors(): MiniMapColors {
    val colors = GraphynDs.colors
    return MiniMapColors(
        background = colors.panelBackground.copy(alpha = 0.92f),
        emptyStroke = colors.border.copy(alpha = 0.4f),
        nodeFill = colors.accent.copy(alpha = 0.22f),
        nodeStroke = colors.accent.copy(alpha = 0.7f),
        viewportFill = colors.selectionRing.copy(alpha = 0.12f),
        viewportStroke = colors.selectionRing.copy(alpha = 0.9f),
    )
}
