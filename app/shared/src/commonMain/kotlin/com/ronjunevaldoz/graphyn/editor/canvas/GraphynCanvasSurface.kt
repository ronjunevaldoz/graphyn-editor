package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private enum class PortSide {
    Input,
    Output,
}

@Composable
fun GraphynCanvasSurface(
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp),
    ) {
        val dotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        GridBackdrop(
            modifier = Modifier.fillMaxSize(),
            dotColor = dotColor,
        )

        val workflow = state.workflow
        if (workflow == null) {
            EmptyCanvasHint()
            return
        }

        ConnectionLayer(
            workflow = workflow,
            state = state,
            modifier = Modifier.fillMaxSize(),
        )

        workflow.nodes.forEachIndexed { index, node ->
            val spec = nodeSpecs.resolve(node.type)
            val position = state.nodePosition(node.id, index)
            CanvasNodeCard(
                modifier = Modifier.offset { position },
                node = node,
                spec = spec,
                selected = state.selectedNodeId == node.id,
                outputs = state.outputsFor(node.id),
                flattenedOutputs = state.flattenedOutputsFor(node.id),
                onClick = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
                onMove = { delta ->
                    state.dispatch(
                        GraphynEditorIntent.MoveNode(
                            nodeId = node.id,
                            delta = delta,
                        ),
                    )
                },
                onBeginConnection = { port ->
                    state.dispatch(GraphynEditorIntent.BeginConnection(node.id, port))
                },
                onCompleteConnection = { port ->
                    state.dispatch(GraphynEditorIntent.CompleteConnection(node.id, port))
                },
                isConnectingFrom = state.connectionDraft?.fromNodeId == node.id,
            )
        }
    }
}

@Composable
private fun GridBackdrop(
    modifier: Modifier,
    dotColor: Color,
) {
    Canvas(modifier = modifier) {
        val spacing = 28.dp.toPx()
        val dotRadius = 1.4.dp.toPx()
        val offset = spacing / 2f

        var x = offset
        while (x < size.width) {
            var y = offset
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(x, y),
                )
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
private fun ConnectionLayer(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
    modifier: Modifier,
) {
    val connectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    Canvas(modifier = modifier) {
        workflow.connections.forEach { connection ->
            val fromNode = workflow.nodes.firstOrNull { it.id == connection.fromNodeId } ?: return@forEach
            val toNode = workflow.nodes.firstOrNull { it.id == connection.toNodeId } ?: return@forEach
            val fromIndex = workflow.nodes.indexOf(fromNode)
            val toIndex = workflow.nodes.indexOf(toNode)
            val fromPosition = state.nodePosition(fromNode.id, fromIndex)
            val toPosition = state.nodePosition(toNode.id, toIndex)

            val start = Offset(
                x = fromPosition.x.toFloat() + GraphynCanvasMetrics.NodeSize.width.toFloat(),
                y = fromPosition.y.toFloat() + GraphynCanvasMetrics.NodeSize.height / 2f,
            )
            val end = Offset(
                x = toPosition.x.toFloat(),
                y = toPosition.y.toFloat() + GraphynCanvasMetrics.NodeSize.height / 2f,
            )
            val distance = (end.x - start.x).absoluteValue.coerceAtLeast(120f)
            val direction = if (end.x >= start.x) 1f else -1f
            val control = distance * 0.35f

            val path = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(
                    start.x + (control * direction),
                    start.y,
                    end.x - (control * direction),
                    end.y,
                    end.x,
                    end.y,
                )
            }

            drawPath(
                path = path,
                color = connectionColor,
                style = Stroke(width = 4f),
            )
        }
    }
}

@Composable
private fun CanvasNodeCard(
    modifier: Modifier,
    node: NodeRef,
    spec: NodeSpec?,
    selected: Boolean,
    outputs: Map<String, WorkflowValue>,
    flattenedOutputs: Map<String, WorkflowValue>,
    onClick: () -> Unit,
    onMove: (IntOffset) -> Unit,
    onBeginConnection: (String) -> Unit,
    onCompleteConnection: (String) -> Unit,
    isConnectingFrom: Boolean,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .sizeIn(
                minWidth = GraphynCanvasMetrics.NodeSize.width.dp,
                maxWidth = GraphynCanvasMetrics.NodeSize.width.dp + 40.dp,
                minHeight = GraphynCanvasMetrics.NodeSize.height.dp,
            )
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .pointerInput(node.id) {
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

            if (spec != null) {
                PortSection(
                    title = "Inputs",
                    ports = spec.inputs.map { "${it.name}:${it.type}" },
                    side = PortSide.Input,
                    onPortClick = onCompleteConnection,
                )
                PortSection(
                    title = "Outputs",
                    ports = spec.outputs.map { "${it.name}:${it.type}" },
                    side = PortSide.Output,
                    onPortClick = onBeginConnection,
                )
            } else {
                Text(
                    text = "No node spec registered yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (outputs.isNotEmpty()) {
                SummarySection(title = "Outputs", text = outputs.keys.joinToString())
            }

            if (flattenedOutputs.isNotEmpty()) {
                SummarySection(title = "Flattened", text = flattenedOutputs.keys.joinToString())
            }

            if (isConnectingFrom) {
                SummarySection(title = "Connection", text = "Draft connection started")
            }
        }
    }
}

@Composable
private fun PortSection(
    title: String,
    ports: List<String>,
    side: PortSide,
    onPortClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        PortBubble(
                            label = port,
                            side = side,
                            onClick = { onPortClick(port) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortBubble(
    label: String,
    side: PortSide,
    onClick: () -> Unit,
) {
    val accent = if (side == PortSide.Input) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val background = accent.copy(alpha = 0.14f)

    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accent, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SummarySection(
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

@Composable
private fun EmptyCanvasHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Add a workflow to start laying out nodes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
