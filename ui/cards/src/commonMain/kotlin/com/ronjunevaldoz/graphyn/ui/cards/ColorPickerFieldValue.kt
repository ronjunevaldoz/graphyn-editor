package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.runtime.Composable

@Composable
internal fun ColorPickerFieldValue(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val launchPicker = LocalColorPickerLauncher.current
    ColorPickerButton(value) { launchPicker(value, onValueChange) }
}
