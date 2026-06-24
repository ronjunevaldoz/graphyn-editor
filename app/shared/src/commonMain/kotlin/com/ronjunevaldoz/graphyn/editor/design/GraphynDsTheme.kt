package com.ronjunevaldoz.graphyn.editor.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ronjunevaldoz.graphyn.core.designsystem.theme.AppTheme
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.GraphynSpacing
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.LocalGraphynSpacing

object GraphynDs {
    val colors: GraphynDsColors
        @Composable @ReadOnlyComposable
        get() = LocalGraphynDsColors.current

    val type: GraphynDsTypography
        @Composable @ReadOnlyComposable
        get() = LocalGraphynDsTypography.current
}

/**
 * Root theme for the Graphyn editor.
 *
 * Wraps [MaterialTheme] intentionally so that Material3 components used inside the editor
 * (e.g. `BasicTextField`, tooltips) receive consistent colours derived from [GraphynDsColors]
 * without requiring a separate `MaterialTheme` call at the host site.
 */
@Composable
fun GraphynDsTheme(
    colors: GraphynDsColors = GraphynDsColors.Dark,
    typography: GraphynDsTypography = GraphynDsTypography.Default,
    content: @Composable () -> Unit,
) {
    val isDark = colors.panelBackground.luminance() < 0.5f
    val materialColors = if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentForeground,
            secondary = colors.portInput,
            onSecondary = Color.White,
            background = colors.canvasBackground,
            onBackground = colors.textPrimary,
            surface = colors.panelBackground,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceCard,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.borderSubtle,
            error = colors.danger,
            onError = colors.dangerForeground,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentForeground,
            secondary = colors.portInput,
            onSecondary = Color.White,
            background = colors.canvasBackground,
            onBackground = colors.textPrimary,
            surface = colors.panelBackground,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceCard,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.borderSubtle,
            error = colors.danger,
            onError = colors.dangerForeground,
        )
    }
    val appTheme = if (isDark) AppTheme.dark() else AppTheme.light()
    AppTheme(theme = appTheme) {
        MaterialTheme(colorScheme = materialColors) {
            CompositionLocalProvider(
                LocalGraphynDsColors provides colors,
                LocalGraphynDsTypography provides typography,
                LocalGraphynSpacing provides GraphynSpacing(),
                content = content,
            )
        }
    }
}

fun GraphynDsColors.Companion.fromPalette(
    palette: com.ronjunevaldoz.graphyn.editor.theme.GraphynPalette,
    dark: Boolean,
): GraphynDsColors {
    val base = if (dark) Dark else Light
    return base.copy(
        accent = palette.primary,
        accentForeground = palette.onPrimary,
        accentHover = palette.primary.copy(alpha = 0.85f),
        danger = palette.error,
        dangerForeground = palette.onError,
        canvasBackground = palette.background,
        panelBackground = palette.surface,
        surfaceCard = palette.surface,
        textPrimary = palette.onSurface,
    )
}
