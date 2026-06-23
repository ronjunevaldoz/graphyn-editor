package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun systemIsDarkTheme(): Boolean {
    val isDark by remember {
        val mq = window.matchMedia("(prefers-color-scheme: dark)")
        mutableStateOf(mq.matches)
    }
    return isDark
}
