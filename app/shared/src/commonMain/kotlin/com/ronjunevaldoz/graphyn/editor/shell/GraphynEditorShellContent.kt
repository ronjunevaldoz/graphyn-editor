package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.editor.ai.GraphynAiAssistantState
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynAiDialog
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynCredentialsDialog
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynPalettePanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynRightRail
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynRightRailTab
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynTopToolbar
import com.ronjunevaldoz.graphyn.editor.state.execute

@Composable
internal fun GraphynEditorShellContent(
    dependencies: GraphynEditorShellDependencies,
    branding: com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding,
    appearanceState: com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState,
    shortcutState: com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState,
    state: com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState,
    canvas: (@Composable () -> Unit)?,
) {
    SideEffect { state.canvasCards = dependencies.canvasCards }
    val colors = GraphynDs.colors
    val executionEngine = dependencies.executionEngine
    val generator = dependencies.workflowGenerator
    var aiOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var rightRailTab by remember {
        mutableStateOf(if (dependencies.sdServerControl != null) GraphynRightRailTab.StableDiffusion else GraphynRightRailTab.Inspector)
    }
    val validator = remember(dependencies.nodeSpecs) { WorkflowGraphValidator(dependencies.nodeSpecs) }
    val assistant = remember(generator, dependencies.nodeSpecs, state, validator) {
        generator?.let {
            GraphynAiAssistantState(
                generator = it,
                catalog = dependencies.nodeSpecs.all(),
                onApply = { wf ->
                    state.withHistory { state.workflow = wf }
                    state.dispatch(GraphynEditorIntent.AutoLayout)
                },
                currentWorkflow = { state.workflow },
                validateWorkflow = validator::validate,
            )
        }
    }
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

    val leftRailWidth = 220.dp
    val rightRailWidth = 280.dp
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(colors.canvasBackground)) {
        val canvasWidth = (maxWidth - leftRailWidth - rightRailWidth).coerceAtLeast(360.dp)
        Column(modifier = Modifier.fillMaxSize()) {
            GraphynTopToolbar(
                branding = branding,
                appearanceState = appearanceState,
                shortcutState = shortcutState,
                canRun = executionEngine != null,
                onRun = { executionEngine?.let { state.execute(it) } },
                onAutoLayout = { state.dispatch(GraphynEditorIntent.AutoLayout) },
                onAutoLayoutBfs = { state.dispatch(GraphynEditorIntent.AutoLayoutBfs) },
                onHome = dependencies.onHome,
                workflowName = if (dependencies.onHome != null) state.workflow?.name else null,
                onToggleAi = if (assistant != null) ({ aiOpen = !aiOpen }) else null,
                aiActive = aiOpen,
                onToggleSettings = dependencies.settingsStore?.let { { settingsOpen = !settingsOpen } },
                settingsActive = settingsOpen,
            )
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                GraphynPalettePanel(
                    modifier = Modifier.width(leftRailWidth).fillMaxHeight(),
                    nodeSpecs = dependencies.nodeSpecs,
                    categoryRegistry = dependencies.categoryRegistry,
                    onAddNode = { spec -> state.dispatch(GraphynEditorIntent.AddNode(spec)) },
                )
                GraphynEditorShellCanvas(
                    state = state,
                    dependencies = dependencies,
                    modifier = Modifier.width(canvasWidth).fillMaxHeight(),
                    canvasContent = canvasContent,
                )
                GraphynRightRail(
                    modifier = Modifier.width(rightRailWidth).fillMaxHeight(),
                    selectedTab = rightRailTab,
                    onSelectTab = { rightRailTab = it },
                    state = state,
                    nodeSpecs = dependencies.nodeSpecs,
                    panels = dependencies.panels,
                    validationErrors = validationErrors,
                    sdServerControl = dependencies.sdServerControl,
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
        if (aiOpen && assistant != null) GraphynAiDialog(assistant = assistant, onDismiss = { aiOpen = false })
        dependencies.settingsStore?.let { store ->
            if (settingsOpen) GraphynCredentialsDialog(store = store, onDismiss = { settingsOpen = false })
        }
    }
}
