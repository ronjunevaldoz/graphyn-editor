@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.bootstrap.GraphynDemoWorkflow
import com.ronjunevaldoz.graphyn.bootstrap.rememberGraphynDemoPanelRegistry
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

@Composable
@Preview
fun App(
    branding: GraphynBranding = GraphynBranding(),
    plugins: List<GraphynPlugin> = emptyList(),
    panels: EditorPanelRegistry? = null,
    executionEngine: WorkflowExecutionEngine? = null,
    initialWorkflow: WorkflowDefinition? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
    appearanceState: GraphynAppearanceState = rememberGraphynAppearanceState(),
) {
    val pluginRegistry = remember(plugins) {
        DefaultGraphynPluginRegistry().apply {
            installAll(plugins)
        }
    }
    val editorPanels = panels ?: remember { DefaultEditorPanelRegistry() }
    val state = rememberGraphynEditorState(
        initialWorkflow = initialWorkflow,
        canvasBounds = canvasBounds,
    )
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appearanceState.resolvedDarkTheme(systemDarkTheme)
    val activePalette = appearanceState.resolvePalette(darkTheme)

    GraphynTheme(
        branding = branding.copy(palette = activePalette),
        darkTheme = darkTheme,
    ) {
        GraphynEditorShell(
            branding = branding,
            dependencies = GraphynEditorShellDependencies(
                nodeSpecs = pluginRegistry.nodeSpecs,
                panels = editorPanels,
                executionEngine = executionEngine,
            ),
            appearanceState = appearanceState,
            state = state,
        )
    }
}

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
    val state = rememberGraphynEditorState(
        initialWorkflow = GraphynDemoWorkflow.initial,
        canvasBounds = canvasBounds,
    )
    val systemDarkTheme = isSystemInDarkTheme()
    val appearanceState = rememberGraphynAppearanceState()
    val darkTheme = appearanceState.resolvedDarkTheme(systemDarkTheme)
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
                executionEngine = executionEngine,
            ),
            appearanceState = appearanceState,
            state = state,
        )
    }
}
