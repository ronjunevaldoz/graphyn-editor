package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
        Modifier.size(28.dp).clip(RoundedCornerShape(appTheme.shapes.xs))
            .background(color)
            .border(1.dp, appTheme.colors.outline, RoundedCornerShape(appTheme.shapes.xs))
            .clickable { onClick() },
    )
}

@Composable
internal fun ColorPickerPalette(onPick: (String) -> Unit) {
    val swatches = listOf("#F55A4A", "#2F6BFF", "#7AA2FF", "#4CAF50", "#F59E0B", "#A855F7", "#FFFFFF", "#111827")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { hex ->
                    val color = hex.toHexColorOrNull() ?: Color.Transparent
                    Box(
                        Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                            .background(color)
                            .border(1.dp, appTheme.colors.outlineVariant, RoundedCornerShape(5.dp))
                            .clickable { onPick(hex) },
                    )
                }
            }
        }
    }
}

internal fun supportsColorPicker(name: String, value: String): Boolean =
    name.contains("color", ignoreCase = true) || value.toHexColorOrNull() != null

internal fun String.toHexColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    val parsed = hex.toLongOrNull(16) ?: return null
    val argb = if (hex.length == 6) (0xFF000000 or parsed) else parsed
    return Color(argb.toInt())
}
