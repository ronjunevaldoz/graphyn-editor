package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun WorkflowLauncherCard(
    template: WorkflowTemplate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText(template.name, style = type.nodeTitle.copy(color = colors.textPrimary))
            template.badge?.let { badge ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF6B6BF7).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    BasicText(badge, style = type.mono.copy(color = Color(0xFF9B9BFF)))
                }
            }
        }
        template.description?.let {
            BasicText(it, style = type.bodySmall.copy(color = colors.textSecondary))
        }
        val nodeCount = template.workflow.nodes.size
        val connCount = template.workflow.connections.size
        if (nodeCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicText("$nodeCount nodes", style = type.mono.copy(color = colors.textDisabled))
                if (connCount > 0) {
                    BasicText("$connCount edges", style = type.mono.copy(color = colors.textDisabled))
                }
            }
        }
    }
}
