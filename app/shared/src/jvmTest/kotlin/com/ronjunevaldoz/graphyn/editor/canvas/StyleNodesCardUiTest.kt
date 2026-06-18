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
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CircleCard
import com.ronjunevaldoz.graphyn.plugins.stylenodes.DarkHeaderCard
import com.ronjunevaldoz.graphyn.plugins.stylenodes.FieldCard
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesSpecs
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

class StyleNodesCardUiTest {

    private val roborazziOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0F),
    )

    private fun kSamplerCtx(selected: Boolean = false) = NodeCanvasContext(
        node = NodeRef(id = "ksampler-1", type = StyleNodesSpecs.kSampler.type),
        spec = StyleNodesSpecs.kSampler,
        selected = selected,
        executionStatus = NodeExecutionStatus.Idle,
        onSelect = {},
        onMove = {},
    )

    private fun distributeCtx(selected: Boolean = false) = NodeCanvasContext(
        node = NodeRef(id = "distribute-1", type = StyleNodesSpecs.distributePoints.type),
        spec = StyleNodesSpecs.distributePoints,
        selected = selected,
        executionStatus = NodeExecutionStatus.Idle,
        onSelect = {},
        onMove = {},
    )

    private fun webhookCtx(selected: Boolean = false) = NodeCanvasContext(
        node = NodeRef(id = "webhook-1", type = StyleNodesSpecs.webhook.type),
        spec = StyleNodesSpecs.webhook,
        selected = selected,
        executionStatus = NodeExecutionStatus.Idle,
        onSelect = {},
        onMove = {},
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun darkHeaderCard_idle() = runDesktopComposeUiTest {
        setContent { Box(modifier = Modifier.padding(16.dp)) { DarkHeaderCard(kSamplerCtx()) } }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun darkHeaderCard_selected() = runDesktopComposeUiTest {
        setContent { Box(modifier = Modifier.padding(16.dp)) { DarkHeaderCard(kSamplerCtx(selected = true)) } }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fieldCard_idle() = runDesktopComposeUiTest {
        setContent { Box(modifier = Modifier.padding(16.dp)) { FieldCard(distributeCtx()) } }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fieldCard_selected() = runDesktopComposeUiTest {
        setContent { Box(modifier = Modifier.padding(16.dp)) { FieldCard(distributeCtx(selected = true)) } }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun circleCard_idle() = runDesktopComposeUiTest {
        setContent { Box(modifier = Modifier.padding(16.dp)) { CircleCard(webhookCtx()) } }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun circleCard_selected() = runDesktopComposeUiTest {
        setContent { Box(modifier = Modifier.padding(16.dp)) { CircleCard(webhookCtx(selected = true)) } }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }
}
