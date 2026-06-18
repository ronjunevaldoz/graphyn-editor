package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynNodePortDots(
    node: NodeRef,
    position: IntOffset,
    spec: NodeSpec,
    inputAnchorYs: List<Int>,
    outputAnchorYs: List<Int>,
    nodeWidthDp: Int,
    state: GraphynEditorState,
    workflow: WorkflowDefinition,
    nodeSpecs: NodeSpecRegistry,
    surfaceColor: Color,
) {
    val draft = state.connectionDraft
    val selectedConn = state.selectedConnection

    spec.outputs.forEachIndexed { portIndex, outputPort ->
        GraphynOutputPortDot(
            node = node,
            portIndex = portIndex,
            outputPort = outputPort,
            position = position,
            anchorYDp = outputAnchorYs.getOrElse(portIndex) { outputAnchorYs.lastOrNull() ?: 0 },
            nodeWidthDp = nodeWidthDp,
            draft = draft,
            workflow = workflow,
            nodeSpecs = nodeSpecs,
            state = state,
            surfaceColor = surfaceColor,
        )
    }

    spec.inputs.forEachIndexed { portIndex, inputPort ->
        GraphynInputPortDot(
            node = node,
            portIndex = portIndex,
            inputPort = inputPort,
            position = position,
            anchorYDp = inputAnchorYs.getOrElse(portIndex) { inputAnchorYs.lastOrNull() ?: 0 },
            draft = draft,
            selectedConn = selectedConn,
            workflow = workflow,
            nodeSpecs = nodeSpecs,
            state = state,
            surfaceColor = surfaceColor,
        )
    }
}
