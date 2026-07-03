package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

internal class ColorPickerRequest(
    val value: String,
    val onPick: (String) -> Unit,
)

internal val LocalColorPickerLauncher = compositionLocalOf<(String, (String) -> Unit) -> Unit> {
    { _, _ -> }
}

@Composable
internal fun FieldCardColorPickerHost(content: @Composable () -> Unit) {
    var request by remember { mutableStateOf<ColorPickerRequest?>(null) }
    CompositionLocalProvider(
        LocalColorPickerLauncher provides { value, onPick ->
            request = ColorPickerRequest(value, onPick)
        },
    ) {
        Box {
            content()
            request?.let { active ->
                Box(Modifier.offset(x = (CARD_WIDTH_DP + 8).dp, y = HEADER_DP.dp).zIndex(20f)) {
                    ColorPickerPopup(active.value) { hex ->
                        active.onPick(hex)
                        request = null
                    }
                }
            }
        }
    }
}
