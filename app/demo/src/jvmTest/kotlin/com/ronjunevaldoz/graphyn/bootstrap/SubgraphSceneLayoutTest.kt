@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.workflows.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class SubgraphSceneLayoutTest {

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
    fun subgraphScene_autoLayout_nodesAreProperlySpaced() = runDesktopComposeUiTest(width = 1920, height = 1080) {
        val editorPlugins = GraphynBootstrap.editorPlugins()
        val runtimePlugins = GraphynBootstrap.runtimePlugins()
        val editorRegistry = DefaultGraphynEditorPluginRegistry().apply { installAll(editorPlugins) }
        val pluginRegistry = DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) }
        val state = GraphynEditorState(WorkflowCatalog.Subgraph.workflow)

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

        runBlocking {
            snapshotFlow { state.canvasSize to state.hasCanvasCards }
                .first { (size, ready) -> size.width > 0 && size.height > 0 && ready }
        }

        runOnIdle {
            state.dispatch(GraphynEditorIntent.AutoLayout)
        }

        runOnIdle {
            val registry = editorRegistry.canvasCards
            val positions = state.nodePositionsByNodeId
            val nodes = WorkflowCatalog.Subgraph.workflow.nodes
            val rects = nodes.mapNotNull { node ->
                val pos = positions[node.id] ?: return@mapNotNull null
                val size = registry.resolve(node.type)
                    ?.let { IntSize(it.nodeWidth, it.nodeHeight) }
                    ?: IntSize(280, 180)
                node.id to Rect(
                    pos.x.toFloat(), pos.y.toFloat(),
                    (pos.x + size.width).toFloat(), (pos.y + size.height).toFloat(),
                )
            }
            for (i in rects.indices) {
                for (j in i + 1 until rects.size) {
                    val (idA, rectA) = rects[i]
                    val (idB, rectB) = rects[j]
                    assertTrue(!rectA.overlaps(rectB), "Nodes '$idA' and '$idB' overlap: $rectA vs $rectB")
                }
            }

            // Fit must map the layout bounding-box centre onto the canvas centre.
            val vp = state.viewport
            val cs = state.canvasSize
            val minX = rects.minOf { it.second.left }
            val maxX = rects.maxOf { it.second.right }
            val minY = rects.minOf { it.second.top }
            val maxY = rects.maxOf { it.second.bottom }
            val screenCx = (minX + maxX) / 2f * vp.scale + vp.offset.x
            val screenCy = (minY + maxY) / 2f * vp.scale + vp.offset.y
            assertTrue(kotlin.math.abs(screenCx - cs.width / 2f) < 1f, "x not centered: $screenCx vs ${cs.width / 2f}")
            assertTrue(kotlin.math.abs(screenCy - cs.height / 2f) < 1f, "y not centered: $screenCy vs ${cs.height / 2f}")
        }

        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
        onNodeWithTag("minimap").captureRoboImage(roborazziOptions = minimapOptions)
    }
}
