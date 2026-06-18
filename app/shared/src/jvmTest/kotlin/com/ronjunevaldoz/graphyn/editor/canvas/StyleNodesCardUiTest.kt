package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.plugins.stylenodes.BlenderNodeCard
import com.ronjunevaldoz.graphyn.plugins.stylenodes.ComfyUiNodeCard
import com.ronjunevaldoz.graphyn.plugins.stylenodes.N8nNodeCard
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesSpecs
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

class StyleNodesCardUiTest {

    private val roborazziOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0F),
    )

    private fun comfyCtx(selected: Boolean = false) = NodeCanvasContext(
        node = NodeRef(id = "ksampler-1", type = StyleNodesSpecs.comfyKSampler.type),
        spec = StyleNodesSpecs.comfyKSampler,
        selected = selected,
        executionStatus = NodeExecutionStatus.Idle,
        onSelect = {},
        onMove = {},
    )

    private fun blenderCtx(selected: Boolean = false) = NodeCanvasContext(
        node = NodeRef(id = "blender-1", type = StyleNodesSpecs.blenderDistribute.type),
        spec = StyleNodesSpecs.blenderDistribute,
        selected = selected,
        executionStatus = NodeExecutionStatus.Idle,
        onSelect = {},
        onMove = {},
    )

    private fun n8nCtx(selected: Boolean = false) = NodeCanvasContext(
        node = NodeRef(id = "webhook-1", type = StyleNodesSpecs.n8nWebhook.type),
        spec = StyleNodesSpecs.n8nWebhook,
        selected = selected,
        executionStatus = NodeExecutionStatus.Idle,
        onSelect = {},
        onMove = {},
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun comfyUiNodeCard_idle() = runDesktopComposeUiTest {
        setContent {
            Box(modifier = Modifier.padding(16.dp)) {
                ComfyUiNodeCard(comfyCtx())
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun comfyUiNodeCard_selected() = runDesktopComposeUiTest {
        setContent {
            Box(modifier = Modifier.padding(16.dp)) {
                ComfyUiNodeCard(comfyCtx(selected = true))
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blenderNodeCard_idle() = runDesktopComposeUiTest {
        setContent {
            Box(modifier = Modifier.padding(16.dp)) {
                BlenderNodeCard(blenderCtx())
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blenderNodeCard_selected() = runDesktopComposeUiTest {
        setContent {
            Box(modifier = Modifier.padding(16.dp)) {
                BlenderNodeCard(blenderCtx(selected = true))
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun n8nNodeCard_idle() = runDesktopComposeUiTest {
        setContent {
            Box(modifier = Modifier.padding(16.dp)) {
                N8nNodeCard(n8nCtx())
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun n8nNodeCard_selected() = runDesktopComposeUiTest {
        setContent {
            Box(modifier = Modifier.padding(16.dp)) {
                N8nNodeCard(n8nCtx(selected = true))
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }
}
