package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.ui.graphics.Color

object GraphynThemePresets {
    val defaults: List<GraphynThemePreset> = listOf(
        GraphynThemePreset(
            id = "n8n",
            label = "n8n",
            lightPalette = GraphynPalette(
                primary = Color(0xFFF55A4A),
                background = Color(0xFFEEF0F5),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1A1B26),
            ),
            darkPalette = GraphynPalette(),
        ),
        GraphynThemePreset(
            id = "graphite",
            label = "Graphite",
            lightPalette = GraphynPalette(
                primary = Color(0xFF2F6BFF),
                background = Color(0xFFF8FAFC),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
            ),
            darkPalette = GraphynPalette(
                primary = Color(0xFF7AA2FF),
                background = Color(0xFF0F172A),
                surface = Color(0xFF111827),
                onSurface = Color(0xFFE5E7EB),
            ),
        ),
    )
}
