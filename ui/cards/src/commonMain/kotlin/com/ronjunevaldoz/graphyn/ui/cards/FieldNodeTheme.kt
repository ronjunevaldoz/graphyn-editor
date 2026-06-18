package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

class FieldNodeTheme(
    val background: @Composable () -> Color = { appTheme.colors.surface },
    val headerBackground: @Composable () -> Color = { appTheme.colors.surfaceVariant },
    val border: @Composable () -> Color = { appTheme.colors.border },
    val selectedBorder: @Composable () -> Color = { appTheme.colors.borderFocus },
    val titleColor: @Composable () -> Color = { appTheme.colors.onSurface },
    val labelColor: @Composable () -> Color = { appTheme.colors.onSurfaceVariant },
    val valueBg: @Composable () -> Color = { appTheme.colors.muted },
    val valueText: @Composable () -> Color = { appTheme.colors.onSurface },
    val divider: @Composable () -> Color = { appTheme.colors.border },
)
