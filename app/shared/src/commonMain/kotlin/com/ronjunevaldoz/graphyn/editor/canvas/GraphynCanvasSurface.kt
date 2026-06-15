package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

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
        val workflow = state.workflow
        if (workflow == null) {
            EmptyCanvasHint()
            return
        }

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
                onClick = { state.selectNode(node.id) },
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
            .sizeIn(minWidth = 240.dp, maxWidth = 320.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
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
                PortSection(title = "Inputs", ports = spec.inputs.map { "${it.name}:${it.type}" })
                PortSection(title = "Outputs", ports = spec.outputs.map { "${it.name}:${it.type}" })
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
        }
    }
}

@Composable
private fun PortSection(
    title: String,
    ports: List<String>,
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ports.take(3).forEach { port ->
                    AssistChip(onClick = {}, label = { Text(port) })
                }
            }
        }
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
