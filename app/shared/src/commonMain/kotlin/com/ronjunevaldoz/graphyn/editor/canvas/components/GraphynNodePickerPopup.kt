package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import kotlin.math.roundToInt

@Composable
internal fun GraphynNodePickerPopup(
    screenPosition: Offset,
    compatibleSpecs: List<Pair<NodeSpec, String>>,
    onPick: (spec: NodeSpec, port: String) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        offset = IntOffset(screenPosition.x.roundToInt(), screenPosition.y.roundToInt()),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 160.dp, max = 260.dp).testTag("node-picker-popup"),
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
        ) {
            Column {
                Text(
                    text = "Add node",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                if (compatibleSpecs.isEmpty()) {
                    Text(
                        text = "No compatible nodes",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    compatibleSpecs.forEach { (spec, port) ->
                        DropdownMenuItem(
                            text = { Text(spec.label) },
                            onClick = { onPick(spec, port) },
                            modifier = Modifier.testTag("node-picker-item-${spec.type}"),
                        )
                    }
                }
            }
        }
    }
}
