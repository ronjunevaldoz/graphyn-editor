package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import kotlin.math.roundToInt

private enum class PortSide {
    Input,
    Output,
}

@Composable
fun GraphynNodeCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onMove: (IntOffset) -> Unit,
    slots: GraphynNodeCardSlots = GraphynNodeCardSlots(),
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val interactionSource = remember(selected) { MutableInteractionSource() }

    Card(
        modifier = modifier
            .size(GraphynCanvasMetrics.NodeSize.width.dp, GraphynCanvasMetrics.NodeSize.height.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .pointerInput(selected) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(
                        IntOffset(
                            x = dragAmount.x.roundToInt(),
                            y = dragAmount.y.roundToInt(),
                        ),
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            with(slots) {
                header()
                body()
                ports()
                footer()
            }
        }
    }
}

@Composable
fun GraphynNodeCardHeader(
    node: NodeRef,
    spec: NodeSpec?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = spec?.label ?: node.type,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = node.id,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun GraphynNodeCardPorts(
    spec: NodeSpec?,
) {
    if (spec == null) {
        Text(
            text = "No node spec registered yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GraphynPortColumn(
            title = "Inputs",
            ports = spec.inputs.map { it.name },
            side = PortSide.Input,
        )
        GraphynPortColumn(
            title = "Outputs",
            ports = spec.outputs.map { it.name },
            side = PortSide.Output,
        )
    }
}

@Composable
fun GraphynNodeCardFooter(
    outputs: Map<String, WorkflowValue>,
    flattenedOutputs: Map<String, WorkflowValue>,
    isConnectingFrom: Boolean,
) {
    if (outputs.isNotEmpty()) {
        GraphynSummarySection(title = "Outputs", text = outputs.keys.joinToString())
    }

    if (flattenedOutputs.isNotEmpty()) {
        GraphynSummarySection(title = "Flattened", text = flattenedOutputs.keys.joinToString())
    }

    if (isConnectingFrom) {
        GraphynSummarySection(title = "Connection", text = "Draft connection started")
    }
}

@Composable
private fun GraphynPortColumn(
    title: String,
    ports: List<String>,
    side: PortSide,
) {
    Column(
        modifier = Modifier.width(122.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ports.isEmpty()) {
            Text(
                text = "None",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ports.take(4).forEach { port ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (side == PortSide.Input) {
                            Arrangement.Start
                        } else {
                            Arrangement.End
                        },
                    ) {
                        GraphynPortBubble(label = port, side = side)
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphynPortBubble(
    label: String,
    side: PortSide,
) {
    val accent = if (side == PortSide.Input) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val background = accent.copy(alpha = 0.14f)

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .border(1.5.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun GraphynSummarySection(
    title: String,
    text: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
