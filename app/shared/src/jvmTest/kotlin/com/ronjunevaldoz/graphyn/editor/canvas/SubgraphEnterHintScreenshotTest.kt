package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.ui.cards.SubgraphCardFactory
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test

// Visual baseline for a subgraph node's dedicated boundary card, including the "↳ Enter" hint.
class SubgraphEnterHintScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private val specs = DefaultNodeSpecRegistry().apply {
        register(NodeSpec("op", "Op",
            inputs = listOf(PortSpec("videos_list", WorkflowType.StringType), PortSpec("video_stitch", WorkflowType.StringType)),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))))
    }

    private fun subgraphNode() = NodeRef(
        id = "sg", type = "graphyn.subgraph",
        subgraph = WorkflowDefinition(
            id = "inner", name = "Batch 1",
            nodes = listOf(NodeRef("b", "op"), NodeRef("c", "op")),
            connections = listOf(ConnectionRef("b", "out", "c", "videos_list")),
        ),
    )

    @Test
    fun subgraphCardWithEnterHintScreenshot() {
        val spec = deriveSubgraphSpec(subgraphNode(), specs)!!
        val factory = SubgraphCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
        val ctx = NodeCanvasContext(
            node = subgraphNode(),
            spec = spec,
            selected = false,
            executionStatus = NodeExecutionStatus.Idle,
            onSelect = {},
            onMove = {},
            onEnterSubgraph = {},
        )

        rule.setContent {
            Box(Modifier.padding(24.dp)) { with(factory) { NodeCanvas(ctx) } }
        }
        rule.waitForIdle()

        val options = RoborazziOptions(
            recordOptions = RoborazziOptions.RecordOptions(resizeScale = 1.0),
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )
        rule.onRoot().captureRoboImage(roborazziOptions = options)
    }
}
