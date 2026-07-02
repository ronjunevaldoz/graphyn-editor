@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.store.ArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin

/**
 * Top-level app entry point that wires runtime plugins, editor plugins, and the launcher/editor
 * screens together.
 */
@Composable
fun GraphynApp(
    branding: GraphynBranding = GraphynBranding(),
    runtimePlugins: List<GraphynPlugin> = GraphynBootstrap.runtimePlugins(),
    editorPlugins: List<GraphynEditorPlugin> = GraphynBootstrap.editorPlugins(),
    executionEngine: WorkflowExecutionEngine? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
    store: WorkflowStore? = null,
    settingsStore: SettingsStore? = null,
    artifactHistory: ArtifactHistory? = null,
    sdServerControl: SdServerControl? = null,
    workflowGenerator: WorkflowGenerator? = null,
) {
    GraphynAppContent(
        branding = branding,
        runtimePlugins = runtimePlugins,
        editorPlugins = editorPlugins,
        executionEngine = executionEngine,
        canvasBounds = canvasBounds,
        store = store,
        settingsStore = settingsStore,
        artifactHistory = artifactHistory,
        sdServerControl = sdServerControl,
        workflowGenerator = workflowGenerator,
    )
}
