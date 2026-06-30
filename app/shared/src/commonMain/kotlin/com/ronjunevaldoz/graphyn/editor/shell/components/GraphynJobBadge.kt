package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlinx.coroutines.delay

/**
 * Pill badge showing live execution status. Fades out when idle and the cursor is away;
 * always visible while nodes are running or for 3 s after a run completes.
 */
@Composable
internal fun GraphynJobBadge(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    val runningCount = state.executionStatusByNodeId.values.count { it == NodeExecutionStatus.Running }
    val lastResult = state.lastExecutionResult
    val hasEverRun = lastResult != null || state.executionStatusByNodeId.isNotEmpty()
    if (!hasEverRun) return

    var showRecent by remember { mutableStateOf(false) }
    LaunchedEffect(lastResult) {
        if (lastResult != null) {
            showRecent = true
            delay(3_000)
            showRecent = false
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetAlpha = if (runningCount > 0 || showRecent || isHovered) 1f else 0f
    val alpha by animateFloatAsState(targetAlpha, tween(600))

    val colors = GraphynDs.colors
    val type = GraphynDs.type

    val (icon, label, tint) = when {
        runningCount > 0 -> Triple("↻", "$runningCount running", colors.accent)
        lastResult?.isFullSuccess == true -> Triple("✓", "Done", Color(0xFF4CAF50))
        lastResult != null && lastResult.errorCount > 0 ->
            Triple("✗", "${lastResult.errorCount} failed", Color(0xFFEF5350))
        else -> Triple("↻", "Idle", colors.textDisabled)
    }

    Row(
        modifier = modifier
            .alpha(alpha)
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.panelBackground.copy(alpha = 0.92f))
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(icon, style = type.label.copy(color = tint))
        BasicText(label, style = type.label.copy(color = colors.textPrimary))
    }
}
