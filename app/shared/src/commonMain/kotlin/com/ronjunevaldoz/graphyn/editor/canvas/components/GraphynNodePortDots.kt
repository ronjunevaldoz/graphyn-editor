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
            draft = draft,
            selectedConn = selectedConn,
            workflow = workflow,
            nodeSpecs = nodeSpecs,
            state = state,
            surfaceColor = surfaceColor,
        )
    }
}
