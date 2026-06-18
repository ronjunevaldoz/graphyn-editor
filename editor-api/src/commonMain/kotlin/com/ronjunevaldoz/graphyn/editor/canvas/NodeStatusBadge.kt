package com.ronjunevaldoz.graphyn.editor.canvas

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus

@Composable
fun NodeStatusBadge(
    status: NodeExecutionStatus,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color(0xFF1E1E1E),
) {
    if (status == NodeExecutionStatus.Idle) return
    val (color, label) = when (status) {
        NodeExecutionStatus.Running -> Color(0xFFFACC15) to "+"
        NodeExecutionStatus.Success -> Color(0xFF4ADE80) to "v"
        NodeExecutionStatus.Error   -> Color(0xFFF87171) to "x"
        NodeExecutionStatus.Idle    -> return
    }
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(surfaceColor)
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = TextStyle(color = color, fontSize = 10.sp))
    }
}
