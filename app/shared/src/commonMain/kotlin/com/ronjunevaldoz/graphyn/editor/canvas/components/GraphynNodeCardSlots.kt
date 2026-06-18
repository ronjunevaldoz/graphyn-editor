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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.PortSpec
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
    val type = GraphynDs.type
    val colors = GraphynDs.colors
    if (spec == null) {
        BasicText("No node spec registered yet.", style = type.bodySmall.copy(color = colors.textDisabled))
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GraphynPortColumn(spec.inputs, PortSide.Input)
        GraphynPortColumn(spec.outputs, PortSide.Output)
    }
}

@Composable
private fun GraphynPortColumn(ports: List<PortSpec>, side: PortSide) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(modifier = Modifier.width(110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val title = if (side == PortSide.Input) "Inputs" else "Outputs"
        BasicText(title, style = type.labelSmall.copy(color = colors.textSecondary))
        if (ports.isEmpty()) {
            BasicText("None", style = type.caption.copy(color = colors.textDisabled))
        } else {
            ports.take(4).forEach { port ->
                val portColor = port.portColor()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (side == PortSide.Input) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (side == PortSide.Input) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(portColor))
                        Box(modifier = Modifier.size(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(portColor.copy(alpha = 0.12f))
                            .border(1.dp, portColor.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        BasicText(port.name, style = type.caption.copy(color = portColor))
                    }
                    if (side == PortSide.Output) {
                        Box(modifier = Modifier.size(4.dp))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(portColor))
                    }
                }
            }
        }
    }
}
