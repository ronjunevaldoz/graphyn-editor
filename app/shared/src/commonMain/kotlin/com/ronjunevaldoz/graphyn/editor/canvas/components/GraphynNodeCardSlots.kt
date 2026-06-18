package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

data class GraphynNodeCardSlots(
    val header: @Composable ColumnScope.() -> Unit = {},
    val body: @Composable ColumnScope.() -> Unit = {},
    val ports: @Composable ColumnScope.() -> Unit = {},
    val footer: @Composable ColumnScope.() -> Unit = {},
)

private enum class PortSide { Input, Output }

@Composable
fun GraphynNodeCardFooter(
    outputs: Map<String, WorkflowValue>,
    flattenedOutputs: Map<String, WorkflowValue>,
    isConnectingFrom: Boolean,
) {
    val type = GraphynDs.type
    val colors = GraphynDs.colors
    if (outputs.isNotEmpty()) {
        BasicText("Outputs: ${outputs.keys.joinToString()}", style = type.caption.copy(color = colors.textDisabled))
    }
    if (isConnectingFrom) {
        BasicText("Connecting…", style = type.caption.copy(color = colors.accent))
    }
}

@Composable
fun GraphynNodeCardPorts(spec: com.ronjunevaldoz.graphyn.core.model.NodeSpec?) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    if (spec == null) {
        BasicText("No node spec registered yet.", style = type.bodySmall.copy(color = colors.textDisabled))
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GraphynPortColumn("Inputs", spec.inputs.map { it.name }, PortSide.Input)
        GraphynPortColumn("Outputs", spec.outputs.map { it.name }, PortSide.Output)
    }
}

@Composable
private fun GraphynPortColumn(title: String, ports: List<String>, side: PortSide) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(modifier = Modifier.width(110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(title, style = type.labelSmall.copy(color = colors.textSecondary))
        if (ports.isEmpty()) {
            BasicText("None", style = type.caption.copy(color = colors.textDisabled))
        } else {
            ports.take(4).forEach { port ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (side == PortSide.Input) Arrangement.Start else Arrangement.End,
                ) {
                    val accent = if (side == PortSide.Input) colors.portInput else colors.portOutput
                    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        BasicText(port, style = type.caption.copy(color = accent))
                    }
                }
            }
        }
    }
}
