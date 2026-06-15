package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private class IosGraphynSettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : GraphynSettingsStore {
    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }
}

@Composable
actual fun rememberGraphynSettingsStore(): GraphynSettingsStore {
    return remember { IosGraphynSettingsStore() }
}
