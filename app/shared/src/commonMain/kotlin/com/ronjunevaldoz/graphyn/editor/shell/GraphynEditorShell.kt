package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynCanvasTelemetryOverlay
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynInspectorPanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynLogPanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynMinimapDebugger
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynPalettePanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynTopToolbar
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState

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
    val canvasContent = canvas ?: {
        GraphynCanvasSurface(
            state = state,
            nodeSpecs = dependencies.nodeSpecs,
        )
    }
    val executionEngine = dependencies.executionEngine
    val validationValidator = remember(dependencies.nodeSpecs) {
        WorkflowGraphValidator(dependencies.nodeSpecs)
    }
    val validationErrors = remember(state.workflow, dependencies.nodeSpecs) {
        state.workflow?.let(validationValidator::validate).orEmpty()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                ),
            ),
    ) {
        GraphynPalettePanel(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.15f),
            nodeSpecs = dependencies.nodeSpecs,
            onAddNode = { spec -> state.dispatch(GraphynEditorIntent.AddNode(spec)) },
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f),
        ) {
            Box(
                modifier = Modifier
                    .weight(.8f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("graphyn-canvas"),
                ) {
                    canvasContent()
                    GraphynCanvasTelemetryOverlay(
                        state = state,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopStart)
                            .padding(12.dp),
                    )
                    GraphynMinimapDebugger(
                        state = state,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomEnd)
                            .size(width = 240.dp, height = 160.dp)
                            .graphicsLayer {
                                alpha = 0.96f
                                clip = true
                            },
                    )
                }
            }
            GraphynLogPanel(
                modifier = Modifier.weight(0.2f),
                state = state
            )
        }
        Column(
            Modifier
                .fillMaxHeight()
                .weight(0.15f)
        ) {

            GraphynTopToolbar(
//                modifier = Modifier.weight(0.2f),
                branding = branding,
                appearanceState = appearanceState,
                canRun = executionEngine != null,
                onRun = {
                    executionEngine?.let { engine -> state.execute(engine) }
                },
            )

            GraphynInspectorPanel(
                modifier = Modifier,
                state = state,
                nodeSpecs = dependencies.nodeSpecs,
                panels = dependencies.panels,
                validationErrors = validationErrors,
            )
        }
    }
}
