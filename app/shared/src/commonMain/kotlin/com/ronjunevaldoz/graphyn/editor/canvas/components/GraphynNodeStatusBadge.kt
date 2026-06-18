package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
fun GraphynNodeStatusBadge(status: NodeExecutionStatus, modifier: Modifier = Modifier) {
    if (status == NodeExecutionStatus.Idle) return
    val (color, label) = when (status) {
        NodeExecutionStatus.Running -> Color(0xFFFACC15) to "●"
        NodeExecutionStatus.Success -> Color(0xFF4ADE80) to "✓"
        NodeExecutionStatus.Error   -> Color(0xFFF87171) to "✕"
        NodeExecutionStatus.Idle    -> return
    }
    val type = GraphynDs.type
    val surface = GraphynDs.colors.surfaceCard
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(surface)
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = type.caption.copy(color = color))
    }
}
