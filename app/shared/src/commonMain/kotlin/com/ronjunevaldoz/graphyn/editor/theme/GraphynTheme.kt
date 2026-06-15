package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Immutable
data class GraphynPalette(
    val primary: Color = Color(0xFF2F6BFF),
    val onPrimary: Color = Color(0xFFFFFFFF),
    val secondary: Color = Color(0xFF334155),
    val onSecondary: Color = Color(0xFFFFFFFF),
    val background: Color = Color(0xFFF8FAFC),
    val onBackground: Color = Color(0xFF0F172A),
    val surface: Color = Color(0xFFFFFFFF),
    val onSurface: Color = Color(0xFF0F172A),
    val error: Color = Color(0xFFB42318),
    val onError: Color = Color(0xFFFFFFFF),
)

@Immutable
data class GraphynBranding(
    val appName: String = "Graphyn",
    val logo: Painter? = null,
    val palette: GraphynPalette = GraphynPalette(),
    val typography: Typography = Typography(),
)

fun GraphynPalette.toColorScheme(darkTheme: Boolean = false): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            error = error,
            onError = onError,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            error = error,
            onError = onError,
        )
    }
}

@Composable
fun GraphynTheme(
    branding: GraphynBranding = GraphynBranding(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = branding.palette.toColorScheme(isSystemInDarkTheme()),
        typography = branding.typography,
        content = content,
    )
}
