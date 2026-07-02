package com.ronjunevaldoz.graphyn.core.common

import androidx.compose.ui.graphics.Color

fun String.toColor(): Color {
    val hex = removePrefix("#")

    val value = when (hex.length) {
        6 -> ("FF$hex").toULong(16)   // RRGGBB
        8 -> hex.toULong(16)          // AARRGGBB
        else -> error("Invalid color: $this. Expected #RRGGBB or #AARRGGBB.")
    }
    return Color(value)
}


fun Color.toHexColor(): String {
    val alpha = (alpha * 255).toInt().coerceIn(0, 255).toHex()
    val red = (red * 255).toInt().coerceIn(0, 255).toHex()
    val green = (green * 255).toInt().coerceIn(0, 255).toHex()
    val blue = (blue * 255).toInt().coerceIn(0, 255).toHex()

    return "#$alpha$red$green$blue"
}

fun Int.toHex(): String =
    toString(16).padStart(2, '0').uppercase()


fun String.isHexColor(): Boolean =
    (length == 7 || length == 9) && startsWith("#") && drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }