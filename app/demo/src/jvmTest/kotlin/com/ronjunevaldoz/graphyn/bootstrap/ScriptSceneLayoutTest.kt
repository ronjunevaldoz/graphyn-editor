@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.script.ScriptEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ScriptSceneLayoutTest {

    private val roborazziOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    )
    private val minimapOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 3.0),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scriptScene_autoLayout_nodesAreProperlySpaced() = runDesktopComposeUiTest(width = 1920, height = 1080) {
        val editorPlugins = GraphynBootstrap.editorPlugins(extraPlugins = listOf(ScriptEditorPlugin))
        val runtimePlugins = GraphynBootstrap.runtimePlugins(extraPlugins = listOf(ScriptPlugin))
        val editorRegistry = DefaultGraphynEditorPluginRegistry().apply { installAll(editorPlugins) }
        val pluginRegistry = DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) }
        val state = GraphynEditorState(DemoScene.Script.workflow)

        setContent {
            GraphynTheme {
                GraphynEditorShell(
                    dependencies = GraphynEditorShellDependencies(
                        nodeSpecs = pluginRegistry.nodeSpecs,
                        canvasCards = editorRegistry.canvasCards,
                    ),
                    state = state,
                    appearanceState = rememberGraphynAppearanceState(),
                )
            }
        }

        // Mirror the exact readiness gate used in DemoApp: both canvas size and canvasCards must be ready.
        runBlocking {
            snapshotFlow { state.canvasSize to state.hasCanvasCards }
                .first { (size, ready) -> size.width > 0 && size.height > 0 && ready }
        }

        runOnIdle {
            state.dispatch(GraphynEditorIntent.AutoLayout)
        }

        // Assert nodes are not overlapping
        runOnIdle {
            val registry = editorRegistry.canvasCards
            val positions = state.nodePositionsByNodeId
            val nodes = DemoScene.Script.workflow.nodes
            val rects = nodes.mapNotNull { node ->
                val pos = positions[node.id] ?: return@mapNotNull null
                val size = registry.resolve(node.type)
                    ?.let { IntSize(it.nodeWidth, it.nodeHeight) }
                    ?: IntSize(280, 180)
                Pair(node.id, androidx.compose.ui.geometry.Rect(
                    pos.x.toFloat(), pos.y.toFloat(),
                    (pos.x + size.width).toFloat(), (pos.y + size.height).toFloat()
                ))
            }
            for (i in rects.indices) {
                for (j in i + 1 until rects.size) {
                    val (idA, rectA) = rects[i]
                    val (idB, rectB) = rects[j]
                    assertTrue(
                        !rectA.overlaps(rectB),
                        "Nodes '$idA' and '$idB' overlap: $rectA vs $rectB"
                    )
                }
            }
            assertTrue(state.viewport.scale <= 1.0f, "scale ${state.viewport.scale} exceeds 1.0f")
        }

        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
        onNodeWithTag("minimap").captureRoboImage(roborazziOptions = minimapOptions)
    }
}
