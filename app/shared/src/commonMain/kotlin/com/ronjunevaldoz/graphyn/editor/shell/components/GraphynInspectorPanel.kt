package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
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
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val selectedNode = remember(state.workflow, state.selectedNodeId) { state.selectedNode() }
    val selectedNodeSpec = remember(selectedNode, nodeSpecs) {
        selectedNode?.let { nodeSpecs.resolve(it.type) }
    }
    val panelFactory = selectedNode?.let { panels.resolve(it.type) }
    val selectedNodeOutputs = selectedNode?.let { state.outputsFor(it.id) }.orEmpty()
    val flattenedOutputs = selectedNode?.let { state.flattenedOutputsFor(it.id) }.orEmpty()
    val selectedConnection = state.selectedConnection

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panelBackground)
            .border(width = 1.dp, color = colors.border),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicText("INSPECTOR", style = type.panelTitle.copy(color = colors.textSecondary))
        }
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (validationErrors.isNotEmpty()) {
                GraphynValidationSummary(errors = validationErrors)
            }
            if (selectedNode != null) {
                BasicText(selectedNode.id, style = type.nodeTitle.copy(color = colors.textPrimary))
                BasicText(selectedNode.type, style = type.bodySmall.copy(color = colors.textSecondary))
                DangerButton(label = "Delete node") {
                    state.dispatch(GraphynEditorIntent.DeleteSelectedNode)
                }
            }
            if (selectedConnection != null) {
                BasicText(
                    "${selectedConnection.fromNodeId}:${selectedConnection.fromPort} → ${selectedConnection.toNodeId}:${selectedConnection.toPort}",
                    style = type.mono.copy(color = colors.textSecondary),
                )
                BasicText("Click an input port to reconnect.", style = type.caption.copy(color = colors.textDisabled))
                DangerButton(label = "Delete connection") {
                    state.dispatch(GraphynEditorIntent.DeleteSelectedConnection)
                }
            }
            if (selectedNode == null && selectedConnection == null) {
                BasicText("Select a node to inspect it.", style = type.bodySmall.copy(color = colors.textDisabled))
            }
            if (panelFactory != null) {
                panelFactory.Content(EditorPanelContext(
                    workflow = state.workflow,
                    selectedNode = selectedNode,
                    selectedNodeSpec = selectedNodeSpec,
                    validationErrors = validationErrors,
                    selectedNodeOutputs = selectedNodeOutputs,
                    flattenedSelectedNodeOutputs = flattenedOutputs,
                ))
            }
        }
    }
}

@Composable
private fun DangerButton(label: String, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = type.label.copy(color = colors.danger))
    }
}

@Composable
internal fun GraphynValidationSummary(errors: List<ValidationError>) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText("${errors.size} validation issue(s)", style = type.label.copy(color = colors.danger))
        errors.take(4).forEach { error ->
            BasicText(error.code, style = type.labelSmall.copy(color = colors.danger))
            BasicText(error.message, style = type.bodySmall.copy(color = colors.textSecondary))
        }
    }
}
