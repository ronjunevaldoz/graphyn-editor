package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
internal fun ColorPickerButton(value: String, onClick: () -> Unit) {
    val color = value.toHexColorOrNull() ?: appTheme.colors.primary
    Box(
        Modifier.size(14.dp).clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(1.dp, appTheme.colors.border, RoundedCornerShape(2.dp))
            .clickable { onClick() },
    )
}

@Composable
internal fun ColorPickerPopup(
    value: String,
    onPick: (String) -> Unit,
) {
    Box(
        Modifier.padding(top = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(appTheme.colors.surface)
            .border(1.dp, appTheme.colors.border, RoundedCornerShape(6.dp))
            .padding(6.dp),
    ) {
        ColorPickerPalette(seed = value, onPick = onPick)
    }
}

@Composable
internal fun ColorPickerPalette(seed: String, onPick: (String) -> Unit) {
    val swatches = generatedSwatches(seed.toHexColorOrNull() ?: appTheme.colors.primary)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { hex ->
                    val color = hex.toHexColorOrNull() ?: Color.Transparent
                    Box(
                        Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(1.dp, appTheme.colors.border, RoundedCornerShape(4.dp))
                            .clickable { onPick(hex) },
                    )
                }
            }
        }
    }
}

internal fun supportsColorPicker(name: String, value: String): Boolean =
    name.contains("color", ignoreCase = true) || value.toHexColorOrNull() != null

private fun generatedSwatches(base: Color): List<String> {
    val hsv = rgbToHsv(base.red, base.green, base.blue)
    if (hsv[1] < 0.12f) {
        return listOf(0f, 24f, 48f, 120f, 180f, 240f)
            .map { hsvToHex(it, 0.72f, 0.92f) } + listOf("#FFFFFF", "#111827")
    }
    return listOf(
        base.toHexString(),
        hsvToHex((hsv[0] + 24f) % 360f, hsv[1], hsv[2]),
        hsvToHex((hsv[0] + 48f) % 360f, hsv[1], hsv[2]),
        hsvToHex((hsv[0] + 120f) % 360f, hsv[1], hsv[2]),
        hsvToHex((hsv[0] + 180f) % 360f, hsv[1], hsv[2]),
        hsvToHex((hsv[0] + 240f) % 360f, hsv[1], hsv[2]),
        hsvToHex(hsv[0], (hsv[1] * 0.65f).coerceIn(0f, 1f), (hsv[2] * 1.15f).coerceIn(0f, 1f)),
        hsvToHex(hsv[0], (hsv[1] * 0.45f).coerceIn(0f, 1f), (hsv[2] * 0.65f).coerceIn(0f, 1f)),
    )
}

internal fun String.toHexColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    val parsed = hex.toLongOrNull(16) ?: return null
    val argb = if (hex.length == 6) (0xFF000000 or parsed) else parsed
    return Color(argb.toInt())
}

private fun Color.toHexString(): String {
    fun channel(value: Float): String = (value.coerceIn(0f, 1f) * 255).toInt().toString(16).padStart(2, '0')
    return "#${channel(red)}${channel(green)}${channel(blue)}".uppercase()
}

private fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val sat = if (max == 0f) 0f else delta / max
    return floatArrayOf(hue, sat, max)
}

private fun hsvToHex(h: Float, s: Float, v: Float): String {
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val color = Color(r1 + m, g1 + m, b1 + m, 1f)
    return color.toHexString()
}
