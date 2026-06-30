package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/** Full-screen overlay for viewing a single job artifact. Click the scrim or ✕ to dismiss. */
@Composable
internal fun GraphynArtifactViewer(item: ArtifactItem, onDismiss: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(12.dp)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 640.dp, height = 480.dp)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(colors.panelBackground)
                .border(1.dp, colors.border, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    BasicText(item.fileName, style = type.nodeTitle.copy(color = colors.textPrimary))
                    BasicText(
                        "${item.nodeLabel} · ${item.portName}",
                        style = type.bodySmall.copy(color = colors.textSecondary),
                    )
                }
                BasicText(
                    "✕",
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                    style = type.bodySmall.copy(color = colors.textDisabled),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth().background(colors.canvasBackground)) {
                ArtifactPreview(item = item, modifier = Modifier.fillMaxSize())
            }
            BasicText(
                item.filePath,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = type.mono.copy(color = colors.textDisabled),
            )
        }
    }
}
