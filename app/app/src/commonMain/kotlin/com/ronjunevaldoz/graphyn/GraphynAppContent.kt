package com.ronjunevaldoz.graphyn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ronjunevaldoz.graphyn.ai.OllamaConfig
import com.ronjunevaldoz.graphyn.ai.OllamaWorkflowGenerator
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.bootstrap.catalogTemplatesFor
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.store.ArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.launcher.GraphynWorkflowLauncher
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowTemplate
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import kotlin.random.Random

@Composable
internal fun GraphynAppContent(
    branding: GraphynBranding,
    runtimePlugins: List<GraphynPlugin>,
    editorPlugins: List<GraphynEditorPlugin>,
    executionEngine: WorkflowExecutionEngine?,
    canvasBounds: GraphynCanvasBounds,
    store: WorkflowStore?,
    settingsStore: SettingsStore?,
    artifactHistory: ArtifactHistory?,
    sdServerControl: SdServerControl?,
    workflowGenerator: WorkflowGenerator?,
) {
    var recentWorkflows by remember { mutableStateOf(emptyList<WorkflowTemplate>()) }
    var savedWorkflows by remember { mutableStateOf(emptyList<WorkflowMeta>()) }
    var openWorkflow by remember { mutableStateOf<WorkflowDefinition?>(null) }
    var openWithStore by remember { mutableStateOf(true) }
    var pendingLoadId by remember { mutableStateOf<String?>(null) }
    var pendingTemplate by remember { mutableStateOf<WorkflowTemplate?>(null) }
    val generator = workflowGenerator ?: remember { OllamaWorkflowGenerator(OllamaConfig(baseUrl = DEMO_OLLAMA_HOST)) }

    LaunchedEffect(store) { savedWorkflows = store?.list() ?: emptyList() }
    LaunchedEffect(pendingLoadId) {
        val id = pendingLoadId ?: return@LaunchedEffect
        store?.load(id)?.let { openWorkflow = it }
        pendingLoadId = null
    }

    val editorRegistry = remember(editorPlugins) { DefaultGraphynEditorPluginRegistry().apply { installAll(editorPlugins) } }
    val pluginRegistry = remember(runtimePlugins) { DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) } }
    val templates = remember(pluginRegistry) { catalogTemplatesFor(pluginRegistry.nodeSpecs) }
    val engine = remember(pluginRegistry) { WorkflowExecutionEngine(pluginRegistry.nodeExecutors, pluginRegistry.nodeSpecs) }
    val appearanceState = rememberGraphynAppearanceState()
    val darkTheme = appearanceState.resolvedDarkTheme(isSystemInDarkTheme())

    GraphynTheme(branding = branding.copy(palette = appearanceState.resolvePalette(darkTheme)), darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize()) {
            val wf = openWorkflow
            if (wf == null) {
                GraphynWorkflowLauncher(
                    templates = templates,
                    recentWorkflows = recentWorkflows,
                    savedWorkflows = savedWorkflows,
                    nodeSpecs = pluginRegistry.nodeSpecs.all(),
                    onNew = {
                        openWithStore = true
                        openWorkflow = WorkflowDefinition("wf-${Random.nextLong().and(0xFFFFFFFFL)}", "Untitled", emptyList(), emptyList())
                    },
                    onOpenSaved = { id -> openWithStore = true; pendingLoadId = id },
                    onOpen = { template ->
                        recentWorkflows = listOf(template) + recentWorkflows.filter { it.workflow.id != template.workflow.id }.take(9)
                        pendingTemplate = template
                    },
                )
            } else {
                key(wf.id) {
                    GraphynEditorSection(
                        wf = wf,
                        store = if (openWithStore) store else null,
                        canvasBounds = canvasBounds,
                        branding = branding,
                        dependencies = GraphynEditorShellDependencies(
                            nodeSpecs = pluginRegistry.nodeSpecs,
                            panels = editorRegistry.panels,
                            canvasCards = editorRegistry.canvasCards,
                            categoryRegistry = editorRegistry.categories,
                            executionEngine = executionEngine ?: engine,
                            workflowGenerator = generator,
                            settingsStore = settingsStore,
                            artifactHistory = artifactHistory,
                            sdServerControl = sdServerControl,
                        ),
                        appearanceState = appearanceState,
                        onHome = { openWorkflow = null; openWithStore = true },
                    )
                }
            }
            pendingTemplate?.let { pending ->
                if (openWorkflow == null) TemplateSaveDialog(
                    templateName = pending.name,
                    onSave = { openWithStore = true; openWorkflow = pending.workflow; pendingTemplate = null },
                    onSkip = { openWithStore = false; openWorkflow = pending.workflow; pendingTemplate = null },
                    onDismiss = { pendingTemplate = null },
                )
            }
        }
    }
}

private const val DEMO_OLLAMA_HOST = OllamaConfig.DEFAULT_BASE_URL
