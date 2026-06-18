package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class CircleNodeTheme(
    val background: @Composable () -> Color = { CIRCLE_BG },
    val selectedBorder: @Composable () -> Color = { NODE_SELECT },
    val labelColor: @Composable () -> Color = { NODE_MUTED },
    val iconColor: @Composable () -> Color = { NODE_TEXT },
)
