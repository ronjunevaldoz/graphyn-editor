package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val APPEARANCE_THEME_MODE_KEY = "graphyn.theme.mode"
private const val APPEARANCE_PRESET_KEY = "graphyn.theme.preset"

class GraphynAppearanceState(
    val presets: List<GraphynThemePreset> = GraphynThemePresets.defaults,
    initialPresetId: String = GraphynThemePresets.defaults.first().id,
    initialThemeMode: GraphynThemeMode = GraphynThemeMode.Dark,
    private val settingsStore: GraphynSettingsStore? = null,
) {
    var selectedPresetId by mutableStateOf(initialPresetId)
        private set
    var themeMode by mutableStateOf(initialThemeMode)
        private set

    val selectedPreset: GraphynThemePreset
        get() = presets.firstOrNull { it.id == selectedPresetId } ?: presets.first()

    fun selectPreset(id: String) {
        if (id == selectedPresetId) return
        selectedPresetId = presets.firstOrNull { it.id == id }?.id ?: selectedPreset.id
        settingsStore?.putString(APPEARANCE_PRESET_KEY, selectedPresetId)
    }

    fun updateThemeMode(mode: GraphynThemeMode) {
        if (mode == themeMode) return
        themeMode = mode
        settingsStore?.putString(APPEARANCE_THEME_MODE_KEY, mode.name)
    }

    fun resolvePalette(dark: Boolean): GraphynPalette =
        if (dark) selectedPreset.darkPalette else selectedPreset.lightPalette

    fun resolvedDarkTheme(system: Boolean): Boolean = when (themeMode) {
        GraphynThemeMode.System -> system
        GraphynThemeMode.Light -> false
        GraphynThemeMode.Dark -> true
    }
}

@Composable
fun rememberGraphynAppearanceState(
    presets: List<GraphynThemePreset> = GraphynThemePresets.defaults,
    initialPresetId: String = GraphynThemePresets.defaults.first().id,
    initialThemeMode: GraphynThemeMode = GraphynThemeMode.Dark,
): GraphynAppearanceState {
    val settingsStore = rememberGraphynSettingsStore()
    val storedPresetId = remember(presets, settingsStore) {
        settingsStore.getString(APPEARANCE_PRESET_KEY)
            ?.takeIf { id -> presets.any { it.id == id } } ?: initialPresetId
    }
    val storedMode = remember(settingsStore) {
        settingsStore.getString(APPEARANCE_THEME_MODE_KEY)
            ?.let { v -> GraphynThemeMode.entries.firstOrNull { it.name == v } }
            ?: initialThemeMode
    }
    return remember(presets, storedPresetId, storedMode, settingsStore) {
        GraphynAppearanceState(presets, storedPresetId, storedMode, settingsStore)
    }
}
