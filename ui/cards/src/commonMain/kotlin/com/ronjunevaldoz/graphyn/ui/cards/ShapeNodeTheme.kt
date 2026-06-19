package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

/**
 * Color overrides for [ShapeCardFactory] cards.
 *
 * All lambdas are `@Composable` so they can read from the active design-system theme.
 */
class ShapeNodeTheme(
    val background: @Composable () -> Color = { appTheme.colors.primary },
    val selectedBorder: @Composable () -> Color = { appTheme.colors.borderFocus },
    val labelColor: @Composable () -> Color = { appTheme.colors.onSurfaceVariant },
    val iconColor: @Composable () -> Color = { appTheme.colors.onPrimary },
)
