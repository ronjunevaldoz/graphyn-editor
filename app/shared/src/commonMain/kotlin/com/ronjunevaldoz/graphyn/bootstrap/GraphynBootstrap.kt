package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPlugin

object GraphynDemoPlugins {
    val runtime: List<GraphynPlugin> = listOf(SampleLoggerPlugin)
    val editor: List<GraphynEditorPlugin> = listOf(SampleLoggerEditorPlugin)
}

@Composable
fun rememberGraphynDemoPanelRegistry(
    editorPlugins: List<GraphynEditorPlugin> = GraphynDemoPlugins.editor,
): EditorPanelRegistry {
    return remember(editorPlugins) {
        DefaultEditorPanelRegistry().apply {
            DefaultGraphynEditorPluginRegistry(this).installAll(editorPlugins)
        }
    }
}
