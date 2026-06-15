package com.ronjunevaldoz.graphyn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine

@Composable
@Preview
fun App(
    branding: GraphynBranding = GraphynBranding(),
    plugins: List<GraphynPlugin> = emptyList(),
    panels: EditorPanelRegistry? = null,
    executionEngine: WorkflowExecutionEngine? = null,
) {
    val pluginRegistry = remember(plugins) {
        DefaultGraphynPluginRegistry().apply {
            installAll(plugins)
        }
    }
    val editorPanels = panels ?: remember { DefaultEditorPanelRegistry() }
    val state = rememberGraphynEditorState()

    GraphynTheme(branding = branding) {
        GraphynEditorShell(
            branding = branding,
            dependencies = GraphynEditorShellDependencies(
                nodeSpecs = pluginRegistry.nodeSpecs,
                panels = editorPanels,
                executionEngine = executionEngine,
            ),
            state = state,
        )
    }
}
