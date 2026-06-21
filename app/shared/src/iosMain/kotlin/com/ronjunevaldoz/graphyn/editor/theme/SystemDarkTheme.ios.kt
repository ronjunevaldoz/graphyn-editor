package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
actual fun systemIsDarkTheme(): Boolean = isSystemInDarkTheme()
