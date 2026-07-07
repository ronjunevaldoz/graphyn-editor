package com.ronjunevaldoz.graphyn.editor.canvas

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.SubgraphNodeCardFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphynPortAnchorResolverTest {

    private val specs = DefaultNodeSpecRegistry().apply {
        register(NodeSpec("op", "Op",
            inputs = listOf(
                PortSpec("first", WorkflowType.StringType),
                PortSpec("second", WorkflowType.StringType),
            ),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))))
    }

    private fun subgraphNode() = NodeRef(
        id = "sg", type = "graphyn.subgraph",
        subgraph = WorkflowDefinition("inner", "Inner",
            nodes = listOf(NodeRef("b", "op"), NodeRef("c", "op")),
            connections = listOf(ConnectionRef("b", "out", "c", "first"))),
    )

    @Test
    fun subgraphConnectionAnchorsMatchTheSubgraphCardDots() {
        // Regression: connections used to fall back to GraphynCanvasMetrics.portAnchorY at index 0
        // because the subgraph type has no registered spec, landing ~55dp below the rendered dots.
        val node = subgraphNode()
        val derived = deriveSubgraphSpec(node, specs)!!
        val card = SubgraphNodeCardFactory(derived.inputs.size, derived.outputs.size)

        derived.inputs.forEachIndexed { i, port ->
            val anchor = resolvePortAnchor(node, port.name, isInput = true, specs, canvasCards = null)
            assertEquals(card.portAnchorY(i, isInput = true, derived), anchor.anchorYDp, "input '${port.name}'")
            assertEquals(card.nodeWidth, anchor.nodeWidthDp)
        }
        derived.outputs.forEachIndexed { i, port ->
            val anchor = resolvePortAnchor(node, port.name, isInput = false, specs, canvasCards = null)
            assertEquals(card.portAnchorY(i, isInput = false, derived), anchor.anchorYDp, "output '${port.name}'")
        }
    }

    @Test
    fun anchorsResolveByPortNameNotDeclarationOrder() {
        val node = NodeRef(id = "n", type = "op")
        val spec = specs.resolve("op")!!
        val second = resolvePortAnchor(node, "second", isInput = true, specs, canvasCards = null)
        val first = resolvePortAnchor(node, "first", isInput = true, specs, canvasCards = null)
        assertEquals(
            com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory(inputRows = 2, outputRows = 1)
                .portAnchorY(1, isInput = true, spec),
            second.anchorYDp,
        )
        assertEquals(true, second.anchorYDp > first.anchorYDp)
    }
}
