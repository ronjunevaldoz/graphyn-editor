package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynThemeMode
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LeftPalette(
            modifier = Modifier.weight(0.22f),
            nodeSpecs = dependencies.nodeSpecs,
            onAddNode = { spec -> state.dispatch(GraphynEditorIntent.AddNode(spec)) },
        )
        Column(modifier = Modifier.weight(0.56f).fillMaxSize()) {
            TopToolbar(
                branding = branding,
                appearanceState = appearanceState,
                canRun = executionEngine != null,
                onRun = {
                    executionEngine?.let { engine -> state.execute(engine) }
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                canvasContent()
            }
            BottomStatusBar()
        }
        RightPanelHost(
            modifier = Modifier.weight(0.22f),
            state = state,
            nodeSpecs = dependencies.nodeSpecs,
            panels = dependencies.panels,
        )
    }
}

@Composable
private fun TopToolbar(
    branding: GraphynBranding,
    appearanceState: GraphynAppearanceState,
    canRun: Boolean,
    onRun: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                branding.logo?.let { logo ->
                    Image(
                        painter = logo,
                        contentDescription = branding.appName,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = branding.appName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canRun) {
                    Button(onClick = onRun) {
                        Text("Run")
                    }
                }
            }
            ThemeControls(appearanceState = appearanceState)
        }
    }
}

@Composable
private fun ThemeControls(
    appearanceState: GraphynAppearanceState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GraphynThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appearanceState.themeMode == mode,
                    onClick = { appearanceState.updateThemeMode(mode) },
                    label = {
                        Text(
                            text = when (mode) {
                                GraphynThemeMode.System -> "System"
                                GraphynThemeMode.Light -> "Light"
                                GraphynThemeMode.Dark -> "Dark"
                            },
                        )
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            appearanceState.presets.forEach { preset ->
                FilterChip(
                    selected = appearanceState.selectedPresetId == preset.id,
                    onClick = { appearanceState.selectPreset(preset.id) },
                    label = { Text(preset.label) },
                )
            }
        }
    }
}

@Composable
private fun LeftPalette(
    modifier: Modifier,
    nodeSpecs: NodeSpecRegistry,
    onAddNode: (NodeSpec) -> Unit,
) {
    Card(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Palette", style = MaterialTheme.typography.titleMedium)
            val specs = nodeSpecs.all()
            if (specs.isEmpty()) {
                Text("No nodes registered yet.")
            } else {
                specs.forEach { spec ->
                    Button(onClick = { onAddNode(spec) }) {
                        Text(spec.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun RightPanelHost(
    modifier: Modifier,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    panels: EditorPanelRegistry,
) {
    val selectedNode = remember(state.workflow, state.selectedNodeId) { state.selectedNode() }
    val selectedNodeSpec = remember(selectedNode, nodeSpecs) {
        selectedNode?.let { nodeSpecs.resolve(it.type) }
    }
    val panelFactory = selectedNode?.let { panels.resolve(it.type) }
    val selectedNodeOutputs = selectedNode?.let { state.outputsFor(it.id) }.orEmpty()
    val flattenedOutputs = selectedNode?.let { state.flattenedOutputsFor(it.id) }.orEmpty()

    Card(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Inspector", style = MaterialTheme.typography.titleMedium)
            if (panelFactory != null) {
                panelFactory.Content(
                    EditorPanelContext(
                        workflow = state.workflow,
                        selectedNode = selectedNode,
                        selectedNodeSpec = selectedNodeSpec,
                        validationErrors = emptyList(),
                        selectedNodeOutputs = selectedNodeOutputs,
                        flattenedSelectedNodeOutputs = flattenedOutputs,
                    ),
                )
            } else {
                Text(
                    text = if (selectedNode == null) {
                        "Select a node to inspect it."
                    } else {
                        "No custom panel registered for '${selectedNode.type}'."
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (selectedNodeOutputs.isNotEmpty()) {
                    Text(
                        text = "Outputs: ${selectedNodeOutputs.keys.joinToString()}",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "Flattened: ${flattenedOutputs.keys.joinToString()}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomStatusBar() {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text(
            text = "Ready",
            modifier = Modifier.padding(16.dp),
        )
    }
}
