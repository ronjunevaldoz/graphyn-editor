package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal const val ZoomStep = 1.15f
internal const val ZoomOutStep = 1f / ZoomStep

@Composable
internal fun GraphynZoomControls(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            onClick = onZoomIn,
            modifier = Modifier
                .size(32.dp)
                .testTag("zoom-in-button"),
            shape = CircleShape,
        ) {
            Text("+", style = MaterialTheme.typography.labelLarge)
        }
        FilledTonalIconButton(
            onClick = onZoomOut,
            modifier = Modifier
                .size(32.dp)
                .testTag("zoom-out-button"),
            shape = CircleShape,
        ) {
            Text("−", style = MaterialTheme.typography.labelLarge)
        }
    }
}
