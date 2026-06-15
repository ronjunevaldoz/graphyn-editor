package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

private class JvmGraphynSettingsStore(
    private val preferences: Preferences = Preferences.userRoot().node("com.ronjunevaldoz.graphyn"),
) : GraphynSettingsStore {
    override fun getString(key: String): String? = preferences.get(key, null)

    override fun putString(key: String, value: String?) {
        if (value == null) {
            preferences.remove(key)
        } else {
            preferences.put(key, value)
        }
    }
}

@Composable
actual fun rememberGraphynSettingsStore(): GraphynSettingsStore {
    return remember { JvmGraphynSettingsStore() }
}
