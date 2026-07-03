package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

@Composable
internal fun ColorPickerFieldValue(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val launchPicker = LocalColorPickerLauncher.current
    // Tracked so the shared card-level popup (FieldCardColorPickerHost) can anchor itself to
    // whichever row's swatch was actually clicked, instead of a fixed card-relative position.
    var anchor by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.onGloballyPositioned { anchor = it.positionInRoot() }) {
        ColorPickerButton(value) { launchPicker(key, value, anchor, onValueChange) }
    }
}
