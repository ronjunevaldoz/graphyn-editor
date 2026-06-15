package com.ronjunevaldoz.graphyn.plugins.sampleloggerui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

object SampleLoggerEditorPanels {
    fun register(registry: EditorPanelRegistry) {
        registry.register("sample.logger", EditorPanelFactory { context ->
            SampleLoggerPanel(context)
        })
    }
}

@Composable
private fun SampleLoggerPanel(context: EditorPanelContext) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Sample Logger Panel",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Node: ${context.selectedNode?.id ?: "none"}",
            )
            Text(
                text = "Type: ${context.selectedNode?.type ?: "none"}",
            )
            val message = context.selectedNodeOutputs["message"] as? WorkflowValue.StringValue
            Text(
                text = "Message: ${message?.value ?: "(no message)"}",
            )
        }
    }
}
