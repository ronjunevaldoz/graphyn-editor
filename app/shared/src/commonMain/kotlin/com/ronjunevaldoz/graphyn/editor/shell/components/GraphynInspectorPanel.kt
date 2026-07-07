package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.connectedInputPorts
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynInspectorPanel(
    modifier: Modifier,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    panels: EditorPanelRegistry,
    validationErrors: List<ValidationError>,
    onEnterSubgraph: ((WorkflowDefinition) -> Unit)? = null,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val selectedNode = remember(state.workflow, state.selectedNodeId) { state.selectedNode() }
    val selectedNodeSpec = remember(selectedNode, nodeSpecs) {
        selectedNode?.let {
            nodeSpecs.resolve(it.type)
                ?: deriveSubgraphSpec(it, nodeSpecs, connectedInputs = state.workflow?.connectedInputPorts(it.id).orEmpty())
        }
    }
    val panelFactory = selectedNode?.let { panels.resolve(it.type) }
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (validationErrors.isNotEmpty()) {
                GraphynValidationSummary(errors = validationErrors)
            }
            when {
                selectedNode != null -> GraphynInspectorNodeSection(
                    node = selectedNode,
                    spec = selectedNodeSpec,
                    state = state,
                    panelFactory = panelFactory,
                    workflow = state.workflow,
                    validationErrors = validationErrors,
                    outputs = state.outputsFor(selectedNode.id),
                    flattenedOutputs = state.flattenedOutputsFor(selectedNode.id),
                    onEnterSubgraph = onEnterSubgraph,
                )
                selectedConnection != null -> GraphynInspectorConnectionSection(
                    connection = selectedConnection,
                    state = state,
                )
                else -> Box(
                    modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    BasicText(
                        "Select a node or connection\nto inspect it.",
                        style = type.bodySmall.copy(color = colors.textDisabled),
                    )
                }
            }
        }
    }
}
