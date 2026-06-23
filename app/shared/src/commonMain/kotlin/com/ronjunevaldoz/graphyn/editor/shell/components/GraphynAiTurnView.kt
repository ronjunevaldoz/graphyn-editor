package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.ai.AiChatTurn
import com.ronjunevaldoz.graphyn.editor.ai.AiTurnStatus
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

/** Renders one assistant turn: the prompt, then the pending/done/error status with warnings. */
@Composable
internal fun AiTurnView(turn: AiChatTurn) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceCard).padding(8.dp),
        ) {
            BasicText("You: ${turn.prompt}", style = type.bodySmall.copy(color = colors.textPrimary))
        }
        when (val s = turn.status) {
            is AiTurnStatus.Pending ->
                BasicText("Generating…", style = type.bodySmall.copy(color = colors.textSecondary), modifier = Modifier.padding(start = 4.dp))
            is AiTurnStatus.Done -> Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText("✓ ${s.summary}", style = type.bodySmall.copy(color = colors.accent))
                s.warning?.let { BasicText("⚠ $it", style = type.bodySmall.copy(color = colors.danger)) }
            }
            is AiTurnStatus.Error ->
                BasicText("✕ ${s.message}", style = type.bodySmall.copy(color = colors.danger), modifier = Modifier.padding(start = 4.dp))
        }
    }
}
