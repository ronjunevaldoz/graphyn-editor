@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.DefaultNodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryRegistry
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.shortcuts.rememberGraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.systemIsDarkTheme
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsColors
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTheme
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTypography
import com.ronjunevaldoz.graphyn.editor.design.fromPalette

data class GraphynEditorShellDependencies(
    val nodeSpecs: NodeSpecRegistry,
    val panels: EditorPanelRegistry = DefaultEditorPanelRegistry(),
    val canvasCards: NodeCanvasRegistry = DefaultNodeCanvasRegistry(),
    val categoryRegistry: NodeCategoryRegistry? = null,
    val executionEngine: WorkflowExecutionEngine? = null,
    /** Called when the user selects "Enter →" on a subgraph node in the inspector. */
    val onEnterSubgraph: ((label: String, inner: WorkflowDefinition) -> Unit)? = null,
    /** Called when the floating exit button is tapped while inside a subgraph canvas. */
    val onExitSubgraph: (() -> Unit)? = null,
    /** Composable rendered at the canvas top-start (e.g. breadcrumb navigation). */
    val canvasTopStart: (@Composable () -> Unit)? = null,
    /** Called when the user taps the "← Home" button in the toolbar; shows the button when set. */
    val onHome: (() -> Unit)? = null,
    /** When set, the toolbar shows an "✨ AI" toggle that opens the workflow-generation panel. */
    val workflowGenerator: com.ronjunevaldoz.graphyn.ai.WorkflowGenerator? = null,
    /** When set, the toolbar shows a "⚙" toggle that opens the credentials/settings dialog. */
    val settingsStore: com.ronjunevaldoz.graphyn.core.store.SettingsStore? = null,
    /** When set, the Artifacts tab offers a History view of all past generations, not just this run. */
    val artifactHistory: com.ronjunevaldoz.graphyn.core.store.ArtifactHistory? = null,
    /** When set, the toolbar shows a live SD server status chip (GPU/VRAM/model) with an unload button. */
    val sdServerControl: com.ronjunevaldoz.graphyn.editor.server.SdServerControl? = null,
)

@Composable
fun GraphynEditorShell(
    dependencies: GraphynEditorShellDependencies,
    branding: GraphynBranding = GraphynBranding(),
    appearanceState: GraphynAppearanceState = rememberGraphynAppearanceState(),
    shortcutState: GraphynShortcutState = rememberGraphynShortcutState(),
    state: GraphynEditorState? = null,
    canvas: (@Composable () -> Unit)? = null,
) {
    val resolvedState = state ?: rememberGraphynEditorState(nodeSpecs = dependencies.nodeSpecs)
    val systemDark = systemIsDarkTheme()
    val isDark = appearanceState.resolvedDarkTheme(systemDark)
    val palette = appearanceState.resolvePalette(isDark)
    val dsColors = remember(palette, isDark) { GraphynDsColors.fromPalette(palette, isDark) }

    GraphynDsTheme(colors = dsColors, typography = GraphynDsTypography.Default) {
        GraphynEditorShellContent(
            dependencies = dependencies,
            branding = branding,
            appearanceState = appearanceState,
            shortcutState = shortcutState,
            state = resolvedState,
            canvas = canvas,
        )
    }
}
