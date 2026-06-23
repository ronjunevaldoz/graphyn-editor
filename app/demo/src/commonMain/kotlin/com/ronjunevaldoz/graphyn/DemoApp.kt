@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ronjunevaldoz.graphyn.bootstrap.DemoScene
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import androidx.compose.runtime.snapshotFlow
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.ai.OllamaConfig
import com.ronjunevaldoz.graphyn.ai.OllamaWorkflowGenerator
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.editor.launcher.GraphynWorkflowLauncher
import com.ronjunevaldoz.graphyn.editor.launcher.NewWorkflowDialog
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowTemplate
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.shell.GraphynSubgraphNavigator
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import kotlinx.coroutines.flow.first

@Composable
fun DemoApp(
    branding: GraphynBranding = GraphynBranding(),
    runtimePlugins: List<GraphynPlugin> = GraphynBootstrap.runtimePlugins(),
    editorPlugins: List<GraphynEditorPlugin> = GraphynBootstrap.editorPlugins(),
    executionEngine: WorkflowExecutionEngine? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
    store: WorkflowStore? = null,
    workflowGenerator: WorkflowGenerator? = null,
) {
    val templates = remember {
        DemoScene.entries.map { WorkflowTemplate(it.label, null, it.workflow) }
    }
    var recentWorkflows by remember { mutableStateOf(emptyList<WorkflowTemplate>()) }
    var savedWorkflows by remember { mutableStateOf(emptyList<WorkflowMeta>()) }
    var openWorkflow by remember { mutableStateOf<WorkflowDefinition?>(null) }
    var pendingLoadId by remember { mutableStateOf<String?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    val generator = workflowGenerator
        ?: remember { OllamaWorkflowGenerator(OllamaConfig(baseUrl = DEMO_OLLAMA_HOST)) }

    LaunchedEffect(store) {
        savedWorkflows = store?.list() ?: emptyList()
    }
    LaunchedEffect(pendingLoadId) {
        val id = pendingLoadId ?: return@LaunchedEffect
        val loaded = store?.load(id)
        if (loaded != null) openWorkflow = loaded
        pendingLoadId = null
    }

    val editorRegistry = remember(editorPlugins) {
        DefaultGraphynEditorPluginRegistry().apply { installAll(editorPlugins) }
    }
    val pluginRegistry = remember(runtimePlugins) {
        DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) }
    }
    val engine = remember(pluginRegistry) {
        WorkflowExecutionEngine(pluginRegistry.nodeExecutors, pluginRegistry.nodeSpecs)
    }
    val appearanceState = rememberGraphynAppearanceState()
    val darkTheme = appearanceState.resolvedDarkTheme(isSystemInDarkTheme())

    GraphynTheme(branding = branding.copy(palette = appearanceState.resolvePalette(darkTheme)), darkTheme = darkTheme) {
        val wf = openWorkflow
        if (wf == null) {
            GraphynWorkflowLauncher(
                templates = templates,
                recentWorkflows = recentWorkflows,
                savedWorkflows = savedWorkflows,
                onNew = { showNewDialog = true },
                onOpenSaved = { id -> pendingLoadId = id },
                onOpen = { template ->
                    recentWorkflows = listOf(template) +
                        recentWorkflows.filter { it.workflow.id != template.workflow.id }.take(9)
                    openWorkflow = template.workflow
                },
            )
            if (showNewDialog) {
                NewWorkflowDialog(
                    generator = generator,
                    catalog = pluginRegistry.nodeSpecs.all(),
                    onCreate = { created -> showNewDialog = false; openWorkflow = created },
                    onDismiss = { showNewDialog = false },
                )
            }
        } else {
            key(wf.id) {
                val state = rememberGraphynEditorState(
                    initialWorkflow = wf,
                    canvasBounds = canvasBounds,
                    store = store,
                )
                if (wf.id == DemoScene.Script.workflow.id) {
                    LaunchedEffect(Unit) {
                        snapshotFlow { state.canvasSize to state.hasCanvasCards }
                            .first { (size, ready) -> size.width > 0 && size.height > 0 && ready }
                        state.dispatch(GraphynEditorIntent.AutoLayout)
                    }
                }
                GraphynSubgraphNavigator(
                    branding = branding,
                    dependencies = GraphynEditorShellDependencies(
                        nodeSpecs = pluginRegistry.nodeSpecs,
                        panels = editorRegistry.panels,
                        canvasCards = editorRegistry.canvasCards,
                        categoryRegistry = editorRegistry.categories,
                        executionEngine = executionEngine ?: engine,
                    ),
                    appearanceState = appearanceState,
                    state = state,
                    onHome = { openWorkflow = null },
                )
            }
        }
    }
}

/** Ollama host used by the demo's AI workflow generation. Override via [DemoApp]'s workflowGenerator. */
private const val DEMO_OLLAMA_HOST = "https://ron-local-home.duckdns.org/ollama/"
