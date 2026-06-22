package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

private val SUCCESS_COLOR = Color(0xFF4ADE80)
private val ERROR_COLOR = Color(0xFFF87171)
private val RUNNING_COLOR = Color(0xFFFACC15)

@Composable
internal fun GraphynExecutionResultSection(
    result: WorkflowExecutionResult,
    statusByNodeId: Map<String, NodeExecutionStatus>,
    nodeLabel: (id: String) -> String,
    modifier: Modifier = Modifier,
) {
    val type = GraphynDs.type
    var expanded by remember(result) { mutableStateOf(true) }

    val successCount = result.executionOrder.count { statusByNodeId[it] == NodeExecutionStatus.Success }
    val allOk = successCount == result.executionOrder.size
    val summaryColor = if (allOk) SUCCESS_COLOR else ERROR_COLOR
    val summaryText = if (allOk) "✓ ${result.executionOrder.size} nodes succeeded"
                      else "✗ $successCount/${result.executionOrder.size} nodes succeeded"

    val headerInteraction = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(interactionSource = headerInteraction, indication = null) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(summaryText, style = type.bodySmall.copy(color = summaryColor))
            BasicText(if (expanded) "▲" else "▼", style = type.bodySmall.copy(color = GraphynDs.colors.textDisabled))
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                result.executionOrder.forEach { nodeId ->
                    NodeResultRow(
                        label = nodeLabel(nodeId),
                        nodeId = nodeId,
                        status = statusByNodeId[nodeId] ?: NodeExecutionStatus.Idle,
                        outputs = result.nodeOutputsByNodeId[nodeId].orEmpty(),
                        subResult = result.subResults[nodeId],
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeResultRow(
    label: String,
    nodeId: String,
    status: NodeExecutionStatus,
    outputs: Map<String, WorkflowValue>,
    subResult: WorkflowExecutionResult?,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val (statusColor, statusIcon) = when (status) {
        NodeExecutionStatus.Success -> SUCCESS_COLOR to "✓"
        NodeExecutionStatus.Error   -> ERROR_COLOR   to "✗"
        NodeExecutionStatus.Running -> RUNNING_COLOR  to "●"
        NodeExecutionStatus.Skipped -> colors.textDisabled to "⊘"
        NodeExecutionStatus.Idle    -> colors.textDisabled to "○"
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(statusIcon, style = type.mono.copy(color = statusColor))
            BasicText(label, style = type.bodySmall.copy(color = colors.textPrimary))
            BasicText("· $nodeId", style = type.mono.copy(color = colors.textDisabled))
        }
        outputs.forEach { (port, value) ->
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("$port →", style = type.mono.copy(color = colors.textSecondary))
                BasicText(value.preview(), style = type.mono.copy(color = colors.accent), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (outputs.isEmpty() && subResult == null) {
            BasicText("  (no outputs)", modifier = Modifier.padding(start = 16.dp),
                style = type.mono.copy(color = colors.textDisabled))
        }
        if (subResult != null) {
            NestedResultSection(subResult = subResult, indent = 16.dp)
        }
    }
}
