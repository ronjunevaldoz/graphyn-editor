package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsColors
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTheme
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTypography
import com.ronjunevaldoz.graphyn.editor.design.fromPalette

enum class GraphynThemeMode { System, Light, Dark }

@Immutable
data class GraphynPalette(
    val primary: Color = Color(0xFFFF6D5A),
    val onPrimary: Color = Color(0xFFFFFFFF),
    val secondary: Color = Color(0xFF7AA2FF),
    val onSecondary: Color = Color(0xFFFFFFFF),
    val background: Color = Color(0xFF1A1B26),
    val onBackground: Color = Color(0xFFE0E0E6),
    val surface: Color = Color(0xFF252630),
    val onSurface: Color = Color(0xFFE0E0E6),
    val error: Color = Color(0xFFFF6B6B),
    val onError: Color = Color(0xFFFFFFFF),
)

@Immutable
data class GraphynBranding(
    val appName: String = "Graphyn",
    val logo: Painter? = null,
    val palette: GraphynPalette = GraphynPalette(),
    val typography: Typography = Typography(),
)

@Immutable
data class GraphynThemePreset(
    val id: String,
    val label: String,
    val lightPalette: GraphynPalette,
    val darkPalette: GraphynPalette,
)

@Composable
fun GraphynTheme(
    branding: GraphynBranding = GraphynBranding(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = remember(branding.palette, darkTheme) {
        GraphynDsColors.fromPalette(branding.palette, darkTheme)
    }
    GraphynDsTheme(colors = colors, typography = GraphynDsTypography.Default, content = content)
}
