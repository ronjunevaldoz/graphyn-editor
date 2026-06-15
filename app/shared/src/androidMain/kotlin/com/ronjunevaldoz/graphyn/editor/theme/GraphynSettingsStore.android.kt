package com.ronjunevaldoz.graphyn.editor.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private class AndroidGraphynSettingsStore(
    context: Context,
) : GraphynSettingsStore {
    private val preferences = context.getSharedPreferences("graphyn-settings", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String?) {
        preferences.edit().apply {
            if (value == null) {
                remove(key)
            } else {
                putString(key, value)
            }
        }.apply()
    }
}

@Composable
actual fun rememberGraphynSettingsStore(): GraphynSettingsStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidGraphynSettingsStore(context)
    }
}
