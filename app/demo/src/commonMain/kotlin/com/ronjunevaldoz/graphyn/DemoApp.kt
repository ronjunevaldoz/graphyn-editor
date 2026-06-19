@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.bootstrap.DemoScene
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasSurface
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin

@Composable
fun DemoApp(
    branding: GraphynBranding = GraphynBranding(),
    runtimePlugins: List<GraphynPlugin> = GraphynBootstrap.runtimePlugins(),
    editorPlugins: List<GraphynEditorPlugin> = GraphynBootstrap.editorPlugins(),
    executionEngine: WorkflowExecutionEngine? = null,
    canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
) {
    var currentScene by remember { mutableStateOf(DemoScene.Styles) }

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
        Column(Modifier.fillMaxSize()) {
            DemoSceneTabBar(currentScene) { currentScene = it }

            key(currentScene) {
                val state = rememberGraphynEditorState(
                    initialWorkflow = currentScene.workflow,
                    canvasBounds = canvasBounds,
                )
                Box(Modifier.weight(1f)) {
                    GraphynEditorShell(
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
                        canvas = {
                            GraphynCanvasSurface(
                                state = state,
                                nodeSpecs = pluginRegistry.nodeSpecs,
                                canvasCards = editorRegistry.canvasCards,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoSceneTabBar(current: DemoScene, onSelect: (DemoScene) -> Unit) {
    val colors = GraphynDs.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.panelBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        DemoScene.entries.forEach { scene ->
            val selected = scene == current
            Box(Modifier.clickable { onSelect(scene) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                BasicText(
                    scene.label,
                    style = TextStyle(
                        color = if (selected) colors.accent else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
