@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsColors
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTheme
import com.ronjunevaldoz.graphyn.editor.design.GraphynDsTypography
import com.ronjunevaldoz.graphyn.editor.design.fromPalette
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.state.execute
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynInspectorPanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynLogPanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynMinimapDebugger
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynPalettePanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynTopToolbar
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynZoomControls
import com.ronjunevaldoz.graphyn.editor.shell.components.ZoomOutStep
import com.ronjunevaldoz.graphyn.editor.shell.components.ZoomStep
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import androidx.compose.ui.geometry.Offset

data class GraphynEditorShellDependencies(
    val nodeSpecs: NodeSpecRegistry,
    val panels: EditorPanelRegistry = DefaultEditorPanelRegistry(),
    val executionEngine: WorkflowExecutionEngine? = null,
)

@Composable
fun GraphynEditorShell(
    dependencies: GraphynEditorShellDependencies,
    branding: GraphynBranding = GraphynBranding(),
    appearanceState: GraphynAppearanceState = rememberGraphynAppearanceState(),
    state: GraphynEditorState = rememberGraphynEditorState(),
    canvas: (@Composable () -> Unit)? = null,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = appearanceState.resolvedDarkTheme(systemDark)
    val palette = appearanceState.resolvePalette(isDark)
    val dsColors = remember(palette, isDark) { GraphynDsColors.fromPalette(palette, isDark) }

    GraphynDsTheme(colors = dsColors, typography = GraphynDsTypography.Default) {
        GraphynEditorShellContent(
            dependencies = dependencies,
            branding = branding,
            appearanceState = appearanceState,
            state = state,
            canvas = canvas,
        )
    }
}

@Composable
private fun GraphynEditorShellContent(
    dependencies: GraphynEditorShellDependencies,
    branding: GraphynBranding,
    appearanceState: GraphynAppearanceState,
    state: GraphynEditorState,
    canvas: (@Composable () -> Unit)?,
) {
    val colors = GraphynDs.colors
    val executionEngine = dependencies.executionEngine
    val validator = remember(dependencies.nodeSpecs) { WorkflowGraphValidator(dependencies.nodeSpecs) }
    val validationErrors = remember(state.workflow, dependencies.nodeSpecs) {
        state.workflow?.let(validator::validate).orEmpty()
    }
    val canvasContent: @Composable () -> Unit = canvas ?: {
        GraphynCanvasSurface(state = state, nodeSpecs = dependencies.nodeSpecs)
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvasBackground)) {
        GraphynTopToolbar(
            branding = branding,
            appearanceState = appearanceState,
            canRun = executionEngine != null,
            onRun = { executionEngine?.let { state.execute(it) } },
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GraphynPalettePanel(
                modifier = Modifier.width(220.dp).fillMaxHeight(),
                nodeSpecs = dependencies.nodeSpecs,
                onAddNode = { spec -> state.dispatch(GraphynEditorIntent.AddNode(spec)) },
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().testTag("graphyn-canvas")) {
                    canvasContent()
                    GraphynZoomControls(
                        modifier = Modifier.align(Alignment.BottomStart),
                        onZoomIn = { state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, ZoomStep, Offset.Zero)) },
                        onZoomOut = { state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, ZoomOutStep, Offset.Zero)) },
                    )
                    GraphynMinimapDebugger(
                        state = state,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 200.dp, height = 130.dp)
                            .testTag("minimap")
                            .graphicsLayer { alpha = 0.9f; clip = true },
                    )
                }
                GraphynLogPanel(modifier = Modifier.fillMaxWidth(), state = state)
            }
            GraphynInspectorPanel(
                modifier = Modifier.width(260.dp).fillMaxHeight(),
                state = state,
                nodeSpecs = dependencies.nodeSpecs,
                panels = dependencies.panels,
                validationErrors = validationErrors,
            )
        }
    }
}
