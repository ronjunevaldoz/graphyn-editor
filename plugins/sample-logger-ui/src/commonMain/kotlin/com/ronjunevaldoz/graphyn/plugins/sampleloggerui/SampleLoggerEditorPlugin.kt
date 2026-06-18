package com.ronjunevaldoz.graphyn.plugins.sampleloggerui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar

object SampleLoggerEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "graphyn.sample.logger.editor",
        displayName = "Sample Logger Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerPanel(
            "sample.logger",
            EditorPanelFactory { context -> SampleLoggerPanel(context) },
        )
    }
}

private val LabelColor = Color(0xFF9B9BA5)
private val ValueColor = Color(0xFFE0E0E0)
private val TitleColor = Color(0xFFE0E0E0)

@Composable
private fun SampleLoggerPanel(context: EditorPanelContext) {
    val message = context.selectedNodeOutputs["message"] as? WorkflowValue.StringValue
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = "LOGGER OUTPUT",
            style = TextStyle(color = LabelColor, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
        )
        PanelRow("Node", context.selectedNode?.id ?: "none")
        PanelRow("Type", context.selectedNode?.type ?: "none")
        PanelRow("Message", message?.value ?: "(no message)")
    }
}

@Composable
private fun PanelRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        BasicText(label, style = TextStyle(color = LabelColor, fontSize = 10.sp))
        BasicText(value, style = TextStyle(color = ValueColor, fontSize = 12.sp))
    }
}
