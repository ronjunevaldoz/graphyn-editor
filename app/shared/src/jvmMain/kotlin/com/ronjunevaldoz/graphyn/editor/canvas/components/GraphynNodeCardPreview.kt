package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.preview.GraphynPreview
import com.ronjunevaldoz.graphyn.preview.GraphynPreviews


private val previewNode = NodeRef(id = "logger-1", type = "sample.logger")
private val previewSpec = NodeSpec(
    type = "sample.logger",
    label = "Logger",
    inputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
    outputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
)

@GraphynPreviews
@Composable
fun GraphynNodeCardPreview() {
    GraphynPreview {
        GraphynNodeCard(
            onMove = {},
            slots = GraphynNodeCardSlots(
                header = { GraphynNodeCardHeader(node = previewNode, spec = previewSpec) },
                ports = { GraphynNodeCardPorts(spec = previewSpec) },
            ),
        )
    }
}

@GraphynPreviews
@Composable
fun GraphynNodeCardSelectedPreview() {
    GraphynPreview {
        GraphynNodeCard(
            selected = true,
            onMove = {},
            slots = GraphynNodeCardSlots(
                header = { GraphynNodeCardHeader(node = previewNode, spec = previewSpec) },
                ports = { GraphynNodeCardPorts(spec = previewSpec) },
            ),
        )
    }
}

@GraphynPreviews
@Composable
fun GraphynNodeCardNoSpecPreview() {
    GraphynPreview {
        GraphynNodeCard(
            onMove = {},
            slots = GraphynNodeCardSlots(
                header = { GraphynNodeCardHeader(node = previewNode, spec = null) },
                ports = { GraphynNodeCardPorts(spec = null) },
            ),
        )
    }
}
