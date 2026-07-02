package com.ronjunevaldoz.graphyn.core.common

import androidx.compose.ui.graphics.Color

fun String.toColor(): Color {
    val hex = removePrefix("#")

    val argb = when (hex.length) {
        6 -> "FF$hex"
        8 -> hex
        else -> error("Invalid color: $this. Expected #RRGGBB or #AARRGGBB.")
    }

    val a = argb.substring(0, 2).toInt(16)
    val r = argb.substring(2, 4).toInt(16)
    val g = argb.substring(4, 6).toInt(16)
    val b = argb.substring(6, 8).toInt(16)

    return Color(
        red = r,
        green = g,
        blue = b,
        alpha = a,
    )
}

  fun Color.toAssColor(): String {
    // ASS color is &HAABBGGRR. Note that Alpha is inverted (00=opaque, FF=transparent)
    val a = (255 - (alpha * 255).toInt()).coerceIn(0, 255).toHex()
    val r = (red * 255).toInt().coerceIn(0, 255).toHex()
    val g = (green * 255).toInt().coerceIn(0, 255).toHex()
    val b = (blue * 255).toInt().coerceIn(0, 255).toHex()

    return "&H$a$b$g$r"
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