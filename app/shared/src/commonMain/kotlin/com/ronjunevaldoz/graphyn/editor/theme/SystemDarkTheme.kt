package com.ronjunevaldoz.graphyn.editor.theme

import androidx.compose.runtime.Composable

/** Returns true if the host OS is currently in dark mode. Platform-specific. */
@Composable
expect fun systemIsDarkTheme(): Boolean
