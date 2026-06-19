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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

private val NESTED_SUCCESS = Color(0xFF4ADE80)

@Composable
internal fun NestedResultSection(subResult: WorkflowExecutionResult, indent: Dp = 16.dp) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.padding(start = indent).fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null) { expanded = !expanded }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            "⊞ ${subResult.executionOrder.size} inner nodes",
            style = type.mono.copy(color = colors.accent),
        )
        BasicText(if (expanded) "▲" else "▼", style = type.mono.copy(color = colors.textDisabled))
    }
    if (expanded) {
        Column(
            modifier = Modifier.padding(start = indent + 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            subResult.executionOrder.forEach { innerId ->
                val outputs = subResult.nodeOutputsByNodeId[innerId].orEmpty()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText("✓", style = type.mono.copy(color = NESTED_SUCCESS))
                    BasicText(innerId, style = type.mono.copy(color = colors.textSecondary))
                }
                outputs.forEach { (port, value) ->
                    Row(
                        modifier = Modifier.padding(start = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText("$port →", style = type.mono.copy(color = colors.textDisabled))
                        BasicText(
                            value.preview(),
                            style = type.mono.copy(color = colors.accent),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
