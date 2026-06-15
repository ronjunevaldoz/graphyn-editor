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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding

data class GraphynEditorShellDependencies(
    val nodeSpecs: NodeSpecRegistry,
    val panels: EditorPanelRegistry = DefaultEditorPanelRegistry(),
)

@Composable
fun GraphynEditorShell(
    dependencies: GraphynEditorShellDependencies,
    branding: GraphynBranding = GraphynBranding(),
    state: GraphynEditorState = rememberGraphynEditorState(),
    canvas: @Composable () -> Unit = { DefaultCanvasPlaceholder() },
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LeftPalette(modifier = Modifier.weight(0.22f), nodeSpecs = dependencies.nodeSpecs)
        Column(modifier = Modifier.weight(0.56f).fillMaxSize()) {
            TopToolbar(branding = branding)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                canvas()
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
private fun TopToolbar(branding: GraphynBranding) {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
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
        }
    }
}

@Composable
private fun LeftPalette(
    modifier: Modifier,
    nodeSpecs: NodeSpecRegistry,
) {
    Card(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Palette", style = MaterialTheme.typography.titleMedium)
            val specs = nodeSpecs.all()
            if (specs.isEmpty()) {
                Text("No nodes registered yet.")
            } else {
                specs.forEach { spec ->
                    Text(spec.label)
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
private fun DefaultCanvasPlaceholder() {
    Card(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Canvas surface goes here",
                modifier = Modifier.padding(24.dp),
            )
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
