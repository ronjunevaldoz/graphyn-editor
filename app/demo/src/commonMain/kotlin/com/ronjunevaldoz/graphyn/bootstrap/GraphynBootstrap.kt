@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesPlugin

object GraphynDemoPlugins {
    val runtime: List<GraphynPlugin> = listOf(SampleLoggerPlugin, StyleNodesPlugin)
    val editor: List<GraphynEditorPlugin> = listOf(SampleLoggerEditorPlugin, StyleNodesEditorPlugin)
}

object GraphynDemoWorkflow {
    val initial: WorkflowDefinition = WorkflowDefinition(
        id = "demo-workflow",
        name = "Demo Workflow",
        nodes = listOf(
            NodeRef(
                id = "logger-1",
                type = "sample.logger",
                config = mapOf(
                    "message" to WorkflowValue.StringValue("Hello from Graphyn"),
                ),
            ),
            NodeRef(
                id = "logger-2",
                type = "sample.logger",
                config = mapOf(
                    "message" to WorkflowValue.StringValue("Connected output"),
                ),
            ),
        ),
        connections = listOf(
            ConnectionRef(
                fromNodeId = "logger-1",
                fromPort = "message",
                toNodeId = "logger-2",
                toPort = "message",
            ),
        ),
    )
}

object GraphynBootstrap {
    fun runtimePlugins(
        extraPlugins: Iterable<GraphynPlugin> = emptyList(),
    ): List<GraphynPlugin> = GraphynDemoPlugins.runtime + extraPlugins

    fun editorPlugins(
        extraPlugins: Iterable<GraphynEditorPlugin> = emptyList(),
    ): List<GraphynEditorPlugin> = GraphynDemoPlugins.editor + extraPlugins
}

@Composable
fun rememberGraphynDemoPanelRegistry(
    editorPlugins: List<GraphynEditorPlugin> = GraphynBootstrap.editorPlugins(),
): EditorPanelRegistry {
    return remember(editorPlugins) {
        DefaultEditorPanelRegistry().apply {
            DefaultGraphynEditorPluginRegistry(this).installAll(editorPlugins)
        }
    }
}
