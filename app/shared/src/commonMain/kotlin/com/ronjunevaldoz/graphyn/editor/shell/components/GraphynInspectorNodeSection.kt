package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynInspectorNodeSection(
    node: NodeRef,
    spec: NodeSpec?,
    state: GraphynEditorState,
    panelFactory: EditorPanelFactory?,
    workflow: WorkflowDefinition?,
    validationErrors: List<ValidationError>,
    outputs: Map<String, WorkflowValue>,
    flattenedOutputs: Map<String, WorkflowValue>,
    onEnterSubgraph: ((WorkflowDefinition) -> Unit)? = null,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InspectorSectionLabel("NODE")
        InspectorCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    spec?.label ?: node.type,
                    style = type.nodeTitle.copy(color = colors.textPrimary),
                )
                spec?.description?.let { BasicText(it, style = type.bodySmall.copy(color = colors.textSecondary)) }
                BasicText(node.type, style = type.bodySmall.copy(color = colors.textSecondary))
                BasicText(node.id, style = type.mono.copy(color = colors.textDisabled))
            }
        }
        if (!spec?.inputs.isNullOrEmpty() || !spec?.outputs.isNullOrEmpty()) {
            InspectorSectionLabel("PORTS")
            InspectorCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    spec?.inputs?.forEach { PortRow(it, isInput = true) }
                    spec?.outputs?.forEach { PortRow(it, isInput = false) }
                }
            }
        }
        if (outputs.isNotEmpty()) {
            InspectorSectionLabel("OUTPUTS")
            InspectorCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    spec?.outputs?.forEach { port ->
                        outputs[port.name]?.let { value ->
                            OutputRow(portName = port.name, value = value)
                        }
                    }
                    outputs.forEach { (key, value) ->
                        if (spec?.outputs?.none { it.name == key } != false) {
                            OutputRow(portName = key, value = value)
                        }
                    }
                }
            }
        }
        val subgraph = node.subgraph
        if (subgraph != null && onEnterSubgraph != null) {
            InspectorSectionLabel("SUBGRAPH")
            InspectorCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText(
                        "${subgraph.nodes.size} nodes · ${subgraph.connections.size} connections",
                        style = type.bodySmall.copy(color = colors.textSecondary),
                    )
                    DangerButton(label = "Enter →") { onEnterSubgraph(subgraph) }
                    DangerButton(label = "Expand ⤢") {
                        state.dispatch(GraphynEditorIntent.ExpandSubgraph(node.id))
                    }
                }
            }
        }
        if (panelFactory != null) {
            panelFactory.Content(
                EditorPanelContext(
                    workflow = workflow,
                    selectedNode = node,
                    selectedNodeSpec = spec,
                    validationErrors = validationErrors,
                    selectedNodeOutputs = outputs,
                    flattenedSelectedNodeOutputs = flattenedOutputs,
                    onConfigChange = { key, value ->
                        state.dispatch(GraphynEditorIntent.UpdateNodeConfig(node.id, key, value))
                    },
                    onEnterSubgraph = onEnterSubgraph,
                ),
            )
        }
        DangerButton(label = "Delete node") {
            state.dispatch(GraphynEditorIntent.DeleteSelectedNode)
        }
    }
}
