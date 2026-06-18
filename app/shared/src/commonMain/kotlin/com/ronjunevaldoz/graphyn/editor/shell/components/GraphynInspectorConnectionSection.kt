package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynInspectorConnectionSection(
    connection: ConnectionRef,
    state: GraphynEditorState,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InspectorSectionLabel("CONNECTION")
        InspectorCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(connection.fromNodeId, style = type.label.copy(color = colors.textPrimary))
                    BasicText("→", style = type.body.copy(color = colors.textDisabled))
                    BasicText(connection.toNodeId, style = type.label.copy(color = colors.textPrimary))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BasicText(connection.fromPort, style = type.mono.copy(color = colors.portOutput))
                    BasicText(connection.toPort, style = type.mono.copy(color = colors.portInput))
                }
                BasicText(
                    "Click an input port to reconnect.",
                    style = type.caption.copy(color = colors.textDisabled),
                )
            }
        }
        DangerButton(label = "Delete connection") {
            state.dispatch(GraphynEditorIntent.DeleteSelectedConnection)
        }
    }
}
