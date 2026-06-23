@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsColors
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTheme
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTypography
import com.ronjunevaldoz.graphyn.editor.design.fromPalette
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.canvas.DefaultNodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryRegistry
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.state.execute
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynInspectorPanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynPalettePanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynTopToolbar
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.shortcuts.rememberGraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.systemIsDarkTheme
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState

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

@Composable
private fun GraphynEditorShellContent(
    dependencies: GraphynEditorShellDependencies,
    branding: GraphynBranding,
    appearanceState: GraphynAppearanceState,
    shortcutState: GraphynShortcutState,
    state: GraphynEditorState,
    canvas: (@Composable () -> Unit)?,
) {
    SideEffect { state.canvasCards = dependencies.canvasCards }
    val colors = GraphynDs.colors
    val executionEngine = dependencies.executionEngine
    val validator = remember(dependencies.nodeSpecs) { WorkflowGraphValidator(dependencies.nodeSpecs) }
    val validationErrors = remember(state.workflow, dependencies.nodeSpecs) {
        state.workflow?.let(validator::validate).orEmpty()
    }
    val canvasContent: @Composable () -> Unit = canvas ?: {
        GraphynCanvasSurface(
            state = state,
            nodeSpecs = dependencies.nodeSpecs,
            canvasCards = dependencies.canvasCards,
            shortcutState = shortcutState,
            onEnterSubgraph = dependencies.onEnterSubgraph,
            onExitSubgraph = dependencies.onExitSubgraph,
            canvasTopStart = dependencies.canvasTopStart,
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvasBackground)) {
        GraphynTopToolbar(
            branding = branding,
            appearanceState = appearanceState,
            shortcutState = shortcutState,
            canRun = executionEngine != null,
            onRun = { executionEngine?.let { state.execute(it) } },
            onAutoLayout = { state.dispatch(GraphynEditorIntent.AutoLayout) },
            onHome = dependencies.onHome,
            workflowName = if (dependencies.onHome != null) state.workflow?.name else null,
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GraphynPalettePanel(
                modifier = Modifier.width(220.dp).fillMaxHeight(),
                nodeSpecs = dependencies.nodeSpecs,
                categoryRegistry = dependencies.categoryRegistry,
                onAddNode = { spec -> state.dispatch(GraphynEditorIntent.AddNode(spec)) },
            )
            GraphynEditorShellCanvas(state = state, dependencies = dependencies, modifier = Modifier.weight(1f), canvasContent = canvasContent)
            GraphynInspectorPanel(
                modifier = Modifier.width(260.dp).fillMaxHeight(),
                state = state,
                nodeSpecs = dependencies.nodeSpecs,
                panels = dependencies.panels,
                validationErrors = validationErrors,
                onEnterSubgraph = dependencies.onEnterSubgraph?.let { callback ->
                    { inner ->
                        val selectedNode = state.selectedNode()
                        val label = selectedNode?.let { dependencies.nodeSpecs.resolve(it.type)?.label ?: it.type }
                            ?: inner.name
                        callback(label, inner)
                    }
                },
            )
        }
    }
}
