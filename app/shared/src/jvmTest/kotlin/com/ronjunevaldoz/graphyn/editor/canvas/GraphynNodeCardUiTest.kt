package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.DemoApp
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCard
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardFooter
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardHeader
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardPorts
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardSlots
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
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
        setContent {
            GraphynNodeCard(
//                modifier = Modifier.offset { position },
//                selected = state.selectedNodeId == node.id,
//                onClick = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
                onMove = { delta ->

                },
                slots = GraphynNodeCardSlots(
                    header = {
//                        GraphynNodeCardHeader(
//                            node = node,
//                            spec = spec,
//                        )
                    },
                    ports = {
//                        GraphynNodeCardPorts(
//                            spec = spec,
//                            onBeginConnection = { port ->
//                                state.dispatch(GraphynEditorIntent.BeginConnection(node.id, port))
//                            },
//                            onCompleteConnection = { port ->
//
//                            },
//                        )
                    },
                    footer = {
//                        GraphynNodeCardFooter(
//                            outputs = state.outputsFor(node.id),
//                            flattenedOutputs = state.flattenedOutputsFor(node.id),
//                            isConnectingFrom = state.connectionDraft?.fromNodeId == node.id,
//                        )
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