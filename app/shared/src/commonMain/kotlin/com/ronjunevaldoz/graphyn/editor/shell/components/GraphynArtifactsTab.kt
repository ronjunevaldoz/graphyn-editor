package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun GraphynArtifactsTab(
    artifacts: List<ArtifactItem>,
    onView: (ArtifactItem) -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    if (artifacts.isEmpty()) {
        BasicText(
            "No artifacts from the last run.",
            style = type.mono.copy(color = colors.textDisabled),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        artifacts.forEach { item ->
            ArtifactRow(item = item, onClick = { onView(item) })
        }
    }
}

@Composable
private fun ArtifactRow(item: ArtifactItem, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArtifactTypeBadge(item.type)
        Column(modifier = Modifier.weight(1f)) {
            BasicText(item.fileName, style = type.bodySmall.copy(color = colors.textPrimary))
            BasicText(item.nodeLabel, style = type.bodySmall.copy(color = colors.textDisabled))
        }
        BasicText("▶", style = type.bodySmall.copy(color = colors.textDisabled))
    }
}

@Composable
private fun ArtifactTypeBadge(artifactType: ArtifactType) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(colors.surfaceCard)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        BasicText(artifactType.label, style = type.bodySmall.copy(color = colors.textSecondary))
    }
}
