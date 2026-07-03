package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlin.math.roundToInt

internal class ColorPickerRequest(
    val key: String,
    val value: String,
    val anchor: Offset,
    val onPick: (String) -> Unit,
)

internal val LocalColorPickerLauncher =
    compositionLocalOf<(String, String, Offset, (String) -> Unit) -> Unit> { { _, _, _, _ -> } }

/**
 * Hosts one color-picker popup shared by every field row on a card, anchored to whichever row's
 * swatch was actually clicked (via [ColorPickerRequest.anchor], the row's position in the window)
 * rather than a fixed card offset, and dismissed via a real [Popup] so outside-click/back-press
 * close it — a plain `Box` overlay has neither of those.
 */
@Composable
internal fun FieldCardColorPickerHost(content: @Composable () -> Unit) {
    var request by remember { mutableStateOf<ColorPickerRequest?>(null) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    val rowGapPx = with(LocalDensity.current) { 18.dp.toPx() }
    CompositionLocalProvider(
        LocalColorPickerLauncher provides { key, value, anchor, onPick ->
            request = if (request?.key == key) null else ColorPickerRequest(key, value, anchor, onPick)
        },
    ) {
        Box(Modifier.onGloballyPositioned { hostOrigin = it.positionInRoot() }) {
            content()
            request?.let { active ->
                val local = active.anchor - hostOrigin
                Popup(
                    offset = IntOffset(local.x.roundToInt(), (local.y + rowGapPx).roundToInt()),
                    onDismissRequest = { request = null },
                ) {
                    ColorPickerPopup(active.value) { hex ->
                        active.onPick(hex)
                        request = null
                    }
                }
            }
        }
    }
}
