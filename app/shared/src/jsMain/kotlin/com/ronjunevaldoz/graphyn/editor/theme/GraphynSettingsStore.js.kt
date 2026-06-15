package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.js.asDynamic
import web.window.window

private class BrowserGraphynSettingsStore : GraphynSettingsStore {
    private val storage = window.asDynamic()["localStorage"]

    override fun getString(key: String): String? = storage.getItem(key)

    override fun putString(key: String, value: String?) {
        if (value == null) {
            storage.removeItem(key)
        } else {
            storage.setItem(key, value)
        }
    }
}

@Composable
actual fun rememberGraphynSettingsStore(): GraphynSettingsStore {
    return remember { BrowserGraphynSettingsStore() }
}
