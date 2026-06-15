package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

private const val APPEARANCE_THEME_MODE_KEY = "graphyn.theme.mode"
private const val APPEARANCE_PRESET_KEY = "graphyn.theme.preset"

enum class GraphynThemeMode {
    System,
    Light,
    Dark,
}

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

@Immutable
data class GraphynThemePreset(
    val id: String,
    val label: String,
    val lightPalette: GraphynPalette,
    val darkPalette: GraphynPalette,
)

object GraphynThemePresets {
    val defaults: List<GraphynThemePreset> = listOf(
        GraphynThemePreset(
            id = "codex",
            label = "Codex",
            lightPalette = GraphynPalette(
                primary = Color(0xFF3457FF),
                onPrimary = Color(0xFFFFFFFF),
                secondary = Color(0xFF475569),
                onSecondary = Color(0xFFFFFFFF),
                background = Color(0xFFF6F5F2),
                onBackground = Color(0xFF111827),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF111827),
                error = Color(0xFFB42318),
                onError = Color(0xFFFFFFFF),
            ),
            darkPalette = GraphynPalette(
                primary = Color(0xFF4F7CFF),
                onPrimary = Color(0xFFFFFFFF),
                secondary = Color(0xFFF59E0B),
                onSecondary = Color(0xFF1B1406),
                background = Color(0xFF0B1020),
                onBackground = Color(0xFFE5E7EB),
                surface = Color(0xFF111827),
                onSurface = Color(0xFFF3F4F6),
                error = Color(0xFFF87171),
                onError = Color(0xFF1F0B0B),
            ),
        ),
        GraphynThemePreset(
            id = "graphite",
            label = "Graphite",
            lightPalette = GraphynPalette(
                primary = Color(0xFF2F6BFF),
                secondary = Color(0xFF334155),
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
            ),
            darkPalette = GraphynPalette(
                primary = Color(0xFF7AA2FF),
                secondary = Color(0xFFCBD5E1),
                background = Color(0xFF0F172A),
                onBackground = Color(0xFFE2E8F0),
                surface = Color(0xFF111827),
                onSurface = Color(0xFFE5E7EB),
            ),
        ),
        GraphynThemePreset(
            id = "forest",
            label = "Forest",
            lightPalette = GraphynPalette(
                primary = Color(0xFF14804A),
                secondary = Color(0xFF0F766E),
                background = Color(0xFFF6FBF7),
                onBackground = Color(0xFF102A17),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF102A17),
            ),
            darkPalette = GraphynPalette(
                primary = Color(0xFF57C785),
                secondary = Color(0xFF7DD3FC),
                background = Color(0xFF08140D),
                onBackground = Color(0xFFD1FAE5),
                surface = Color(0xFF0B1B12),
                onSurface = Color(0xFFE2F7EA),
            ),
        ),
        GraphynThemePreset(
            id = "sunset",
            label = "Sunset",
            lightPalette = GraphynPalette(
                primary = Color(0xFFD97706),
                secondary = Color(0xFFB45309),
                background = Color(0xFFFEFAF2),
                onBackground = Color(0xFF2B1600),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF2B1600),
            ),
            darkPalette = GraphynPalette(
                primary = Color(0xFFF59E0B),
                secondary = Color(0xFFF97316),
                background = Color(0xFF1A1208),
                onBackground = Color(0xFFFDE68A),
                surface = Color(0xFF24170C),
                onSurface = Color(0xFFFFF7ED),
            ),
        ),
    )
}

class GraphynAppearanceState(
    val presets: List<GraphynThemePreset> = GraphynThemePresets.defaults,
    initialPresetId: String = GraphynThemePresets.defaults.first().id,
    initialThemeMode: GraphynThemeMode = GraphynThemeMode.System,
    private val settingsStore: GraphynSettingsStore? = null,
) {
    var selectedPresetId by mutableStateOf(initialPresetId)
        private set
    var themeMode by mutableStateOf(initialThemeMode)
        private set

    val selectedPreset: GraphynThemePreset
        get() = presets.firstOrNull { it.id == selectedPresetId } ?: presets.first()

    fun selectPreset(presetId: String) {
        if (presetId == selectedPresetId) return
        selectedPresetId = presets.firstOrNull { it.id == presetId }?.id ?: selectedPreset.id
        settingsStore?.putString(APPEARANCE_PRESET_KEY, selectedPresetId)
    }

    fun updateThemeMode(mode: GraphynThemeMode) {
        if (mode == themeMode) return
        themeMode = mode
        settingsStore?.putString(APPEARANCE_THEME_MODE_KEY, mode.name)
    }

    fun resolvePalette(darkTheme: Boolean): GraphynPalette {
        return if (darkTheme) {
            selectedPreset.darkPalette
        } else {
            selectedPreset.lightPalette
        }
    }

    fun resolvedDarkTheme(systemDarkTheme: Boolean): Boolean {
        return when (themeMode) {
            GraphynThemeMode.System -> systemDarkTheme
            GraphynThemeMode.Light -> false
            GraphynThemeMode.Dark -> true
        }
    }
}

@Composable
fun rememberGraphynAppearanceState(
    presets: List<GraphynThemePreset> = GraphynThemePresets.defaults,
    initialPresetId: String = GraphynThemePresets.defaults.first().id,
    initialThemeMode: GraphynThemeMode = GraphynThemeMode.System,
): GraphynAppearanceState {
    val settingsStore = rememberGraphynSettingsStore()
    val storedPresetId = remember(presets, settingsStore) {
        settingsStore.getString(APPEARANCE_PRESET_KEY)
            ?.takeIf { candidate -> presets.any { it.id == candidate } }
            ?: initialPresetId
    }
    val storedThemeMode = remember(settingsStore) {
        settingsStore.getString(APPEARANCE_THEME_MODE_KEY)
            ?.let { value -> GraphynThemeMode.entries.firstOrNull { it.name == value } }
            ?: initialThemeMode
    }

    return remember(presets, storedPresetId, storedThemeMode, settingsStore) {
        GraphynAppearanceState(
            presets = presets,
            initialPresetId = storedPresetId,
            initialThemeMode = storedThemeMode,
            settingsStore = settingsStore,
        )
    }
}

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = branding.palette.toColorScheme(darkTheme),
        typography = branding.typography,
        content = content,
    )
}
