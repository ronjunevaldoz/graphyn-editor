package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.ui.cards.ShapeCardFactory
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/**
 * Renders a ShapeCard node (as used by Gmail/LinkedIn) together with the port-anchor markers
 * the canvas would draw: an input dot at x=0 and an output dot at x=nodeWidth, both at
 * y=portAnchorY. If the circle does not sit between the two markers, the card is misaligned
 * with its own ports. The label is intentionally wider than the circle to expose the bug.
 */
class ShapeNodeAlignmentTest {

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0F),
    )

    private fun spec(label: String) = NodeSpec(
        type = "gmail.fetch_emails",
        label = label,
        inputs = listOf(PortSpec("trigger", WorkflowType.OpaqueType, required = false)),
        outputs = listOf(PortSpec("emails", WorkflowType.OpaqueType, required = false)),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shapeNode_withPortAnchors() = runDesktopComposeUiTest {
        val factory = ShapeCardFactory(shape = CircleShape, size = 48.dp)
        val s = spec("Fetch Emails")
        val ctx = NodeCanvasContext(
            node = NodeRef(id = "n1", type = s.type),
            spec = s,
            selected = false,
            executionStatus = NodeExecutionStatus.Idle,
            onSelect = {},
            onMove = {},
        )
        val anchorY = factory.portAnchorY(0, true, s)
        val nodeWidth = factory.nodeWidth
        setContent {
            Box(modifier = Modifier.padding(40.dp)) {
                with(factory) { NodeCanvas(ctx) }
                // input anchor (red) at left edge, output anchor (green) at nodeWidth
                Box(
                    Modifier
                        .offset { IntOffset(-3.dp.roundToPx(), anchorY.dp.roundToPx() - 3.dp.roundToPx()) }
                        .size(6.dp)
                        .background(Color.Red)
                )
                Box(
                    Modifier
                        .offset { IntOffset(nodeWidth.dp.roundToPx() - 3.dp.roundToPx(), anchorY.dp.roundToPx() - 3.dp.roundToPx()) }
                        .size(6.dp)
                        .background(Color.Green)
                )
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shapeNode_gallery_variedLabels() = runDesktopComposeUiTest {
        // Representative Gmail/LinkedIn node labels — all use the same ShapeCardFactory, so the
        // only variable affecting alignment is label width. Each node carries port anchors.
        val labels = listOf(
            "Send", "Fetch Emails", "Get Labels", "Reply to Email", "Share a LinkedIn Post",
        )
        val factory = ShapeCardFactory(shape = CircleShape, size = 48.dp)
        val anchorY = factory.portAnchorY(0, true, spec("x"))
        val nodeWidth = factory.nodeWidth
        setContent {
            Column(
                modifier = Modifier.padding(40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                labels.forEach { label ->
                    val s = spec(label)
                    val ctx = NodeCanvasContext(
                        node = NodeRef(id = label, type = s.type),
                        spec = s,
                        selected = false,
                        executionStatus = NodeExecutionStatus.Idle,
                        onSelect = {},
                        onMove = {},
                    )
                    Box {
                        with(factory) { NodeCanvas(ctx) }
                        Box(
                            Modifier
                                .offset { IntOffset(-3.dp.roundToPx(), anchorY.dp.roundToPx() - 3.dp.roundToPx()) }
                                .size(6.dp).background(Color.Red)
                        )
                        Box(
                            Modifier
                                .offset { IntOffset(nodeWidth.dp.roundToPx() - 3.dp.roundToPx(), anchorY.dp.roundToPx() - 3.dp.roundToPx()) }
                                .size(6.dp).background(Color.Green)
                        )
                    }
                }
            }
        }
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }
}
