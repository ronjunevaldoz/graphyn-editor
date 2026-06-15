package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable

interface GraphynSettingsStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

@Composable
expect fun rememberGraphynSettingsStore(): GraphynSettingsStore
