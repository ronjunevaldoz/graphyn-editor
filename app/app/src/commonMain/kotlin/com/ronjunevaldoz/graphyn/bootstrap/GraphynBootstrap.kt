@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.runtime.GraphynRuntime
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.stickynotes.StickyNoteEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.stickynotes.StickyNotePlugin
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesPlugin

/**
 * Demo plugin set = the production [GraphynRuntime] plugins plus sample/demonstration plugins
 * (logger, style-node card shapes, sticky notes, subgraph) that are not part of a real deployment.
 */
object GraphynDemoPlugins {
    private val demoOnlyRuntime: List<GraphynPlugin> =
        listOf(SampleLoggerPlugin, StyleNodesPlugin, StickyNotePlugin, SubgraphRuntimePlugin, DemoSceneRuntimePlugin)
    private val demoOnlyEditor: List<GraphynEditorPlugin> =
        listOf(SampleLoggerEditorPlugin, StyleNodesEditorPlugin, StickyNoteEditorPlugin, SubgraphEditorPlugin, DemoSceneEditorPlugin)

    val runtime: List<GraphynPlugin> = GraphynRuntime.runtimePlugins + demoOnlyRuntime
    val editor: List<GraphynEditorPlugin> = GraphynRuntime.editorPlugins + demoOnlyEditor
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
