package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory
import kotlin.test.Test
import kotlin.test.assertEquals

// A subgraph node renders as the standard FieldCardFactory (see GraphynNodeFactoryResolver) —
// this test proves the card's generic double-tap-to-enter-subgraph gesture still works for it.
class SubgraphNodeCardUiTest {

    private val specs = DefaultNodeSpecRegistry().apply {
        register(NodeSpec("op", "Op",
            inputs = listOf(PortSpec("in", WorkflowType.StringType)),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))))
    }

    // A subgraph node whose inner workflow exposes one free input and one free output.
    private fun subgraphNode() = NodeRef(
        id = "sg", type = "graphyn.subgraph",
        subgraph = WorkflowDefinition(
            id = "inner", name = "Inner",
            nodes = listOf(NodeRef("b", "op"), NodeRef("c", "op")),
            connections = listOf(ConnectionRef("b", "out", "c", "in")),
        ),
    )

    private fun ctx(onEnter: (() -> Unit)?, onSelect: () -> Unit = {}) =
        NodeCanvasContext(
            node = subgraphNode(),
            spec = deriveSubgraphSpec(subgraphNode(), specs)!!,
            selected = false,
            executionStatus = NodeExecutionStatus.Idle,
            onSelect = onSelect,
            onMove = {},
            onEnterSubgraph = onEnter,
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun doubleClickEntersSubgraph() = runDesktopComposeUiTest {
        var entered = 0
        val spec = deriveSubgraphSpec(subgraphNode(), specs)!!
        val factory = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
        setContent {
            Box(Modifier.padding(16.dp)) { with(factory) { NodeCanvas(ctx(onEnter = { entered++ })) } }
        }
        onNodeWithTag("node-header-sg").performTouchInput { doubleClick() }
        assertEquals(1, entered, "double-click should invoke onEnterSubgraph once")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun enterHintIsVisibleOnlyWhenOnEnterSubgraphIsProvided() = runDesktopComposeUiTest {
        val spec = deriveSubgraphSpec(subgraphNode(), specs)!!
        val factory = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size, hasEnterHint = true)
        setContent {
            Box(Modifier.padding(16.dp)) { with(factory) { NodeCanvas(ctx(onEnter = {})) } }
        }
        onNodeWithText("↳ Enter").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun enterHintIsHiddenWithoutHasEnterHintOrOnEnterSubgraph() = runDesktopComposeUiTest {
        val spec = deriveSubgraphSpec(subgraphNode(), specs)!!
        val hintReserved = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size, hasEnterHint = true)
        val hintNotReserved = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
        setContent {
            Box(Modifier.padding(16.dp)) {
                with(hintReserved) { NodeCanvas(ctx(onEnter = null)) } // hasEnterHint but no callback
                with(hintNotReserved) { NodeCanvas(ctx(onEnter = {})) } // callback but not reserved
            }
        }
        assertEquals(0, onAllNodesWithText("↳ Enter").fetchSemanticsNodes().size)
    }
}
