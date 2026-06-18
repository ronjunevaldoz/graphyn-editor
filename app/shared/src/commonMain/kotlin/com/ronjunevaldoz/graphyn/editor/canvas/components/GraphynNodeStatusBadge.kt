package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
fun GraphynNodeStatusBadge(status: NodeExecutionStatus, modifier: Modifier = Modifier) {
    NodeStatusBadge(status = status, modifier = modifier, surfaceColor = GraphynDs.colors.surfaceCard)
}
