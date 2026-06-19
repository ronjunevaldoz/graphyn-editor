package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.displayName
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
                ),
            )
        }
        DangerButton(label = "Delete node") {
            state.dispatch(GraphynEditorIntent.DeleteSelectedNode)
        }
    }
}

@Composable
private fun OutputRow(portName: String, value: WorkflowValue) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BasicText(portName, style = type.bodySmall.copy(color = colors.textPrimary))
        BasicText(
            text = value.preview(),
            style = type.mono.copy(color = colors.accent),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun WorkflowValue.preview(): String = when (this) {
    is WorkflowValue.StringValue  -> if (value.length > 40) "\"${value.take(37)}…\"" else "\"$value\""
    is WorkflowValue.IntValue     -> value.toString()
    is WorkflowValue.DoubleValue  -> value.toString()
    is WorkflowValue.BooleanValue -> value.toString()
    is WorkflowValue.ListValue    -> "[${items.size} items]"
    is WorkflowValue.RecordValue  -> "{${fields.size} fields}"
    WorkflowValue.NullValue       -> "null"
    WorkflowValue.OpaqueValue     -> "opaque"
}

@Composable
private fun PortRow(port: PortSpec, isInput: Boolean) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(
                text = "${if (isInput) "→" else "←"} ${port.name}",
                style = type.bodySmall.copy(color = colors.textPrimary),
            )
            BasicText(
                text = port.type.displayName(),
                style = type.mono.copy(color = colors.textDisabled),
            )
        }
        port.description?.let {
            BasicText(it, style = type.bodySmall.copy(color = colors.textSecondary))
        }
    }
}

