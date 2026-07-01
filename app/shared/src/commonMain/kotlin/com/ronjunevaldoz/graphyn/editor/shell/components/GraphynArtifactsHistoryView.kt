package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.store.ArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.ArtifactRecord
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/**
 * Artifacts tab with a This Run / History switch. "This Run" shows [thisRun] (the last execution's
 * outputs); "History" lists every past generation from [history], so artifacts outlive the run that
 * produced them. When [history] is null the switch is hidden and only the current run is shown.
 */
@Composable
internal fun GraphynArtifactsHistoryView(
    thisRun: List<ArtifactItem>,
    history: ArtifactHistory?,
    onView: (ArtifactItem) -> Unit,
) {
    var showHistory by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf<List<ArtifactItem>>(emptyList()) }
    LaunchedEffect(showHistory) {
        if (showHistory && history != null) historyItems = history.list().map { it.toArtifactItem() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (history != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SwitchChip("This Run", !showHistory) { showHistory = false }
                SwitchChip("History", showHistory) { showHistory = true }
            }
        }
        val items = if (showHistory) historyItems else thisRun
        if (showHistory && items.isEmpty()) {
            BasicText("No artifacts yet.", style = GraphynDs.type.mono.copy(color = GraphynDs.colors.textDisabled))
        } else {
            GraphynArtifactsTab(artifacts = items, onView = onView)
        }
    }
}

@Composable
private fun SwitchChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier.clip(shape)
            .background(if (active) colors.accent else colors.panelBackground)
            .border(1.dp, if (active) colors.accent else colors.border, shape)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(label, style = type.label.copy(color = if (active) colors.accentForeground else colors.textSecondary))
    }
}

private fun ArtifactRecord.toArtifactItem(): ArtifactItem = ArtifactItem(
    nodeId = id,
    nodeLabel = workflowName ?: nodeType?.substringAfterLast('.') ?: "history",
    portName = listOfNotNull(
        elapsedMs?.let { "${it / 1000}s" },
        model?.substringBeforeLast('.'),
        prompt?.take(24),
    ).joinToString(" · "),
    filePath = path,
    type = when (kind) {
        ArtifactKind.Image -> ArtifactType.Image
        ArtifactKind.Video -> ArtifactType.Video
        ArtifactKind.Audio -> ArtifactType.Audio
    },
)
