package com.ronjunevaldoz.graphyn.editor.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GraphynDsColors(
    val canvasBackground: Color,
    val panelBackground: Color,
    val surfaceCard: Color,
    val surfaceRaised: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accent: Color,
    val accentForeground: Color,
    val accentHover: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val dangerForeground: Color,
    val portInput: Color,
    val portOutput: Color,
    val connectionLine: Color,
    val selectionRing: Color,
) {
    companion object {
        val Dark = GraphynDsColors(
            canvasBackground = Color(0xFF1A1B26),
            panelBackground = Color(0xFF252630),
            surfaceCard = Color(0xFF2D2E3D),
            surfaceRaised = Color(0xFF343546),
            border = Color(0xFF3D3E52),
            borderSubtle = Color(0xFF2D2E40),
            textPrimary = Color(0xFFE0E0E6),
            textSecondary = Color(0xFF9B9BA5),
            textDisabled = Color(0xFF5A5A6A),
            accent = Color(0xFFFF6D5A),
            accentForeground = Color(0xFFFFFFFF),
            accentHover = Color(0xFFFF8A7A),
            success = Color(0xFF53D98A),
            warning = Color(0xFFF59E0B),
            danger = Color(0xFFFF6B6B),
            dangerForeground = Color(0xFFFFFFFF),
            portInput = Color(0xFF7AA2FF),
            portOutput = Color(0xFFFF6D5A),
            connectionLine = Color(0xFFFF6D5A),
            selectionRing = Color(0xFF7AA2FF),
        )

        val Light = GraphynDsColors(
            canvasBackground = Color(0xFFEEF0F5),
            panelBackground = Color(0xFFFFFFFF),
            surfaceCard = Color(0xFFFFFFFF),
            surfaceRaised = Color(0xFFF8F9FB),
            border = Color(0xFFDDE1EA),
            borderSubtle = Color(0xFFEFF1F6),
            textPrimary = Color(0xFF1A1B26),
            textSecondary = Color(0xFF6B7280),
            textDisabled = Color(0xFFB0B5BE),
            accent = Color(0xFFF55A4A),
            accentForeground = Color(0xFFFFFFFF),
            accentHover = Color(0xFFE04035),
            success = Color(0xFF1DB67A),
            warning = Color(0xFFF59E0B),
            danger = Color(0xFFDC2626),
            dangerForeground = Color(0xFFFFFFFF),
            portInput = Color(0xFF4F7CFF),
            portOutput = Color(0xFFF55A4A),
            connectionLine = Color(0xFFF55A4A),
            selectionRing = Color(0xFF4F7CFF),
        )
    }
}

val LocalGraphynDsColors = compositionLocalOf { GraphynDsColors.Dark }
