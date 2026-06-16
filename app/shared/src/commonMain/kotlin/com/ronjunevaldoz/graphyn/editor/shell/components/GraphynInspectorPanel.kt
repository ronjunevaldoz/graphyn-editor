package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynInspectorPanel(
    modifier: Modifier,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    panels: EditorPanelRegistry,
    validationErrors: List<ValidationError>,
) {
    val selectedNode = remember(state.workflow, state.selectedNodeId) { state.selectedNode() }
    val selectedNodeSpec = remember(selectedNode, nodeSpecs) {
        selectedNode?.let { nodeSpecs.resolve(it.type) }
    }
    val panelFactory = selectedNode?.let { panels.resolve(it.type) }
    val selectedNodeOutputs = selectedNode?.let { state.outputsFor(it.id) }.orEmpty()
    val flattenedOutputs = selectedNode?.let { state.flattenedOutputsFor(it.id) }.orEmpty()

    GraphynChromePanel(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Inspector", style = MaterialTheme.typography.titleMedium)
            GraphynValidationSummary(validationErrors = validationErrors)
            if (panelFactory != null) {
                panelFactory.Content(
                    com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext(
                        workflow = state.workflow,
                        selectedNode = selectedNode,
                        selectedNodeSpec = selectedNodeSpec,
                        validationErrors = validationErrors,
                        selectedNodeOutputs = selectedNodeOutputs,
                        flattenedSelectedNodeOutputs = flattenedOutputs,
                    ),
                )
            } else {
                Text(
                    text = if (selectedNode == null) {
                        "Select a node to inspect it."
                    } else {
                        "No custom panel registered for '${selectedNode.type}'."
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (selectedNodeOutputs.isNotEmpty()) {
                    Text(
                        text = "Outputs: ${selectedNodeOutputs.keys.joinToString()}",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "Flattened: ${flattenedOutputs.keys.joinToString()}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GraphynValidationSummary(
    validationErrors: List<ValidationError>,
) {
    if (validationErrors.isEmpty()) {
        Text(
            text = "Workflow valid.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${validationErrors.size} validation issue(s)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        validationErrors.take(4).forEach { error ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = error.code,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
