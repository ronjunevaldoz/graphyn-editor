package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private class BrowserGraphynSettingsStore : GraphynSettingsStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String?) {
        if (value == null) {
            values.remove(key)
        } else {
            values[key] = value
        }
    }
}

@Composable
actual fun rememberGraphynSettingsStore(): GraphynSettingsStore {
    return remember { BrowserGraphynSettingsStore() }
}
