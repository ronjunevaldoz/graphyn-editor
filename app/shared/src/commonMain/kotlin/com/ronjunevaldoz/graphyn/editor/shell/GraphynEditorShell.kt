package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.calculateMinimapLayout
import com.ronjunevaldoz.graphyn.editor.state.calculateViewportRectInMinimap
import com.ronjunevaldoz.graphyn.editor.state.mapMinimapPointToWorld
import com.ronjunevaldoz.graphyn.editor.state.constrainTo
import com.ronjunevaldoz.graphyn.editor.state.viewportCenteredOnWorldPoint
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynThemeMode
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import kotlin.math.roundToInt

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                ),
            ),
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("graphyn-canvas"),
                ) {
                    canvasContent()
                    CanvasTelemetryOverlay(
                        state = state,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    )
                    MiniMapDebugger(
                        state = state,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(width = 240.dp, height = 160.dp)
                            .graphicsLayer {
                                alpha = 0.96f
                                shadowElevation = 8f
                                clip = true
                            },
                    )
                }
            }
            LogPanel(state = state)
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
    ChromePanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 12.dp, end = 12.dp),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
    ChromePanel(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Palette", style = MaterialTheme.typography.titleMedium)
            val specs = nodeSpecs.all()
            if (specs.isEmpty()) {
                Text("No nodes registered yet.")
            } else {
                specs.forEach { spec ->
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        onClick = { onAddNode(spec) },
                    ) {
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

    ChromePanel(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
private fun CanvasTelemetryOverlay(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    ChromePanel(
        modifier = modifier,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Canvas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Scale ${formatScale(state.viewport.scale)}x",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "Offset ${formatOffset(state.viewport.offset)}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = "Pan: drag canvas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOverlayButton(
                    text = "Zoom +",
                    tag = "zoom-in-button",
                    onClick = {
                        state.dispatch(
                            GraphynEditorIntent.UpdateViewportTransform(
                                pan = androidx.compose.ui.geometry.Offset.Zero,
                                zoom = 1.15f,
                                focus = androidx.compose.ui.geometry.Offset.Zero,
                            ),
                        )
                        state.addDebugLog("Zoomed in to ${formatScale(state.viewport.scale)}x")
                    },
                )
                SmallOverlayButton(
                    text = "Zoom -",
                    tag = "zoom-out-button",
                    onClick = {
                        state.dispatch(
                            GraphynEditorIntent.UpdateViewportTransform(
                                pan = androidx.compose.ui.geometry.Offset.Zero,
                                zoom = 1f / 1.15f,
                                focus = androidx.compose.ui.geometry.Offset.Zero,
                            ),
                        )
                        state.addDebugLog("Zoomed out to ${formatScale(state.viewport.scale)}x")
                    },
                )
                SmallOverlayButton(
                    text = "Reset",
                    tag = "reset-viewport-button",
                    onClick = { state.resetViewport() },
                )
            }
        }
    }
}

@Composable
private fun SmallOverlayButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .height(30.dp)
            .testTag(tag),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun LogPanel(state: GraphynEditorState) {
    ChromePanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleMedium,
            )
            val logs = state.debugLogEntries
            if (logs.isEmpty()) {
                Text(
                    text = "No log entries yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    logs.takeLast(8).forEach { entry ->
                        Text(
                            text = "• $entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatScale(scale: Float): String = ((scale * 100).roundToInt() / 100f).toString()

private fun formatOffset(offset: androidx.compose.ui.geometry.Offset): String {
    val x = (offset.x * 10).roundToInt() / 10f
    val y = (offset.y * 10).roundToInt() / 10f
    return "(${x}, ${y})"
}

@Composable
private fun MiniMapDebugger(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    val workflow = state.workflow
    val nodeCount = workflow?.nodes?.size ?: 0
    val minimapColors = rememberMinimapColors()
    var minimapSize by remember { mutableStateOf(IntSize.Zero) }
    val nodePositions = remember(workflow, state.nodePositionsByNodeId) {
        workflow?.nodes.orEmpty().mapIndexed { index, node ->
            state.nodePosition(node.id, index)
        }
    }
    val graphWorldBounds = state.graphWorldBounds
    val minimapLayout = remember(graphWorldBounds, minimapSize) {
        val worldBounds = graphWorldBounds ?: return@remember null
        calculateMinimapLayout(
            worldBounds = worldBounds,
            minimapSize = minimapSize,
        )
    }
    ChromePanel(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Minimap",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$nodeCount nodes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("minimap")
                    .onSizeChanged { minimapSize = it }
                    .pointerInput(minimapLayout, state.canvasSize) {
                        val layout = minimapLayout ?: return@pointerInput
                        var dragActive = false
                        detectDragGestures(
                            onDragStart = { start ->
                                dragActive = true
                                val worldPoint = mapMinimapPointToWorld(
                                    minimapPoint = start,
                                    layout = layout,
                                )
                                state.viewport = viewportCenteredOnWorldPoint(
                                    currentViewport = state.viewport,
                                    canvasSize = state.canvasSize,
                                    worldPoint = worldPoint,
                                ).constrainTo(state.graphWorldBounds, state.canvasSize)
                            },
                            onDragEnd = { dragActive = false },
                            onDragCancel = { dragActive = false },
                        ) { change, dragAmount ->
                            if (!dragActive) return@detectDragGestures
                            change.consume()
                            val worldPoint = mapMinimapPointToWorld(
                                minimapPoint = change.position,
                                layout = layout,
                            )
                            state.viewport = viewportCenteredOnWorldPoint(
                                currentViewport = state.viewport,
                                canvasSize = state.canvasSize,
                                worldPoint = worldPoint,
                            ).constrainTo(state.graphWorldBounds, state.canvasSize)
                        }
                    },
            ) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                drawRect(color = minimapColors.background)

                if (nodePositions.isEmpty()) {
                    drawRect(
                        color = minimapColors.emptyStroke,
                        style = Stroke(width = 1f),
                    )
                    return@Canvas
                }

                val layout = minimapLayout

                if (layout == null) {
                    drawRect(
                        color = minimapColors.emptyStroke,
                        style = Stroke(width = 1f),
                    )
                    return@Canvas
                }

                nodePositions.forEach { position ->
                    val x = layout.insetX + ((position.x.toFloat() - layout.worldBounds.left) * layout.scale)
                    val y = layout.insetY + ((position.y.toFloat() - layout.worldBounds.top) * layout.scale)
                    val width = GraphynCanvasMetrics.NodeSize.width * layout.scale
                    val height = GraphynCanvasMetrics.NodeSize.height * layout.scale
                    drawRect(
                        color = minimapColors.nodeFill,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(width, height),
                    )
                    drawRect(
                        color = minimapColors.nodeStroke,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        style = Stroke(width = 1.5f),
                    )
                }

                calculateViewportRectInMinimap(
                    viewport = state.viewport,
                    canvasSize = state.canvasSize,
                    layout = layout,
                )?.let { viewportRect ->
                    val cameraColor = minimapColors.viewportStroke
                    val topLeft = Offset(viewportRect.left, viewportRect.top)
                    val size = androidx.compose.ui.geometry.Size(
                        viewportRect.width,
                        viewportRect.height,
                    )
                    drawRect(
                        color = cameraColor,
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(width = 2f),
                    )
                    val handleRadius = 2.25f
                    listOf(
                        topLeft,
                        Offset(topLeft.x + size.width, topLeft.y),
                        Offset(topLeft.x, topLeft.y + size.height),
                        Offset(topLeft.x + size.width, topLeft.y + size.height),
                    ).forEach { corner ->
                        drawCircle(
                            color = cameraColor,
                            radius = handleRadius,
                            center = corner,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromePanel(
    modifier: Modifier = Modifier,
    tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = tonalElevation,
        shadowElevation = tonalElevation,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        content()
    }
}

@Composable
private fun rememberMinimapColors(): MiniMapColors = MiniMapColors(
    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
    emptyStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
    nodeFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
    nodeStroke = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
    viewportStroke = MaterialTheme.colorScheme.secondary.copy(alpha = 0.96f),
)

private data class MiniMapColors(
    val background: Color,
    val emptyStroke: Color,
    val nodeFill: Color,
    val nodeStroke: Color,
    val viewportStroke: Color,
)
