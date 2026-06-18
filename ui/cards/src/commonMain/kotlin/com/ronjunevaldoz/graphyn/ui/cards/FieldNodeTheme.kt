package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class FieldNodeTheme(
    val background: @Composable () -> Color = { NODE_BG },
    val headerBackground: @Composable () -> Color = { FIELD_HEADER_BG },
    val border: @Composable () -> Color = { NODE_BORDER },
    val selectedBorder: @Composable () -> Color = { NODE_SELECT },
    val titleColor: @Composable () -> Color = { NODE_TEXT },
    val labelColor: @Composable () -> Color = { NODE_MUTED },
    val valueBg: @Composable () -> Color = { FIELD_VALUE_BG },
    val valueText: @Composable () -> Color = { NODE_TEXT },
    val divider: @Composable () -> Color = { NODE_BORDER },
)
