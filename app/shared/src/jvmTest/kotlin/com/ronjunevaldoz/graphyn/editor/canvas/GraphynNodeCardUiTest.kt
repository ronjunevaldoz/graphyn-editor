package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCard
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardPorts
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardSlots
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/**
 *  TODO GraphynNodeCard to make sure the desired layout matches
 *  Placeholder for now, this will help to test if the ports are properly connected
 *  screenshot location at /app/shared/build/output/roborazzi
 */
class GraphynNodeCardUiTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun test() = runDesktopComposeUiTest {
        val spec = NodeSpec(
            type = "sample.logger",
            label = "Logger",
            inputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
            outputs = listOf(PortSpec(name = "message", type = WorkflowType.StringType, required = false)),
        )
        setContent {
            GraphynNodeCard(
                onMove = {},
                slots = GraphynNodeCardSlots(
                    ports = {
                        GraphynNodeCardPorts(spec = spec)
                    },
                )
            )
        }
        val roborazziOptions = RoborazziOptions(
            recordOptions = RoborazziOptions.RecordOptions(
                resizeScale = 0.5
            ),
            compareOptions = RoborazziOptions.CompareOptions(
                changeThreshold = 0F
            )
        )
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)

//        onNodeWithTag("button").performClick()
//
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }
}