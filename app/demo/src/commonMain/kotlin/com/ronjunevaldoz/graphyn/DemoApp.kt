@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.bootstrap.GraphynDemoWorkflow
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin

@Composable
fun DemoApp(
    branding: GraphynBranding = GraphynBranding(),
    runtimePlugins: List<GraphynPlugin> = GraphynBootstrap.runtimePlugins(),
    editorPlugins: List<GraphynEditorPlugin> = GraphynBootstrap.editorPlugins(),
    executionEngine: WorkflowExecutionEngine? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
) {
    val editorRegistry = remember(editorPlugins) {
        DefaultGraphynEditorPluginRegistry().apply { installAll(editorPlugins) }
    }
    val pluginRegistry = remember(runtimePlugins) {
        DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) }
    }
    val engine = remember(pluginRegistry) {
        WorkflowExecutionEngine(pluginRegistry.nodeExecutors, pluginRegistry.nodeSpecs)
    }
    val state = rememberGraphynEditorState(
        initialWorkflow = GraphynDemoWorkflow.initial,
        canvasBounds = canvasBounds,
    )
    val appearanceState = rememberGraphynAppearanceState()
    val darkTheme = appearanceState.resolvedDarkTheme(isSystemInDarkTheme())
    val activePalette = appearanceState.resolvePalette(darkTheme)

    GraphynTheme(
        branding = branding.copy(palette = activePalette),
        darkTheme = darkTheme,
    ) {
        GraphynEditorShell(
            branding = branding,
            dependencies = GraphynEditorShellDependencies(
                nodeSpecs = pluginRegistry.nodeSpecs,
                panels = editorRegistry.panels,
                canvasCards = editorRegistry.canvasCards,
                executionEngine = executionEngine ?: engine,
            ),
            appearanceState = appearanceState,
            state = state,
        )
    }
}
