package com.ronjunevaldoz.graphyn.editor.canvas

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
import kotlin.test.assertTrue

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
    fun subgraphConnectionAnchorsMatchTheStandardFieldCardDots() {
        // Regression: connections used to fall back to GraphynCanvasMetrics.portAnchorY at index 0
        // because the subgraph type has no registered spec, landing ~55dp below the rendered dots.
        // Subgraph nodes render via the standard FieldCardFactory (same as any other node).
        val node = subgraphNode()
        val derived = deriveSubgraphSpec(node, specs)!!
        val card = FieldCardFactory(inputRows = derived.inputs.size, outputRows = derived.outputs.size)

        derived.inputs.forEachIndexed { i, port ->
            val anchor = resolvePortAnchor(node, port.name, isInput = true, specs, canvasCards = null, workflow = null)
            assertEquals(card.portAnchorY(i, isInput = true, derived), anchor.anchorYDp, "input '${port.name}'")
            assertEquals(card.nodeWidth, anchor.nodeWidthDp)
        }
        derived.outputs.forEachIndexed { i, port ->
            val anchor = resolvePortAnchor(node, port.name, isInput = false, specs, canvasCards = null, workflow = null)
            assertEquals(card.portAnchorY(i, isInput = false, derived), anchor.anchorYDp, "output '${port.name}'")
        }
    }

    @Test
    fun connectedOptionalSubgraphInputStaysAddressableByConnections() {
        val optSpecs = DefaultNodeSpecRegistry().apply {
            register(NodeSpec("op2", "Op2",
                inputs = listOf(
                    PortSpec("in", WorkflowType.StringType),
                    PortSpec("tweak", WorkflowType.StringType, required = false),
                ),
                outputs = listOf(PortSpec("out", WorkflowType.StringType))))
        }
        val sg = NodeRef(
            id = "sg", type = "graphyn.subgraph",
            subgraph = WorkflowDefinition("inner", "Inner",
                nodes = listOf(NodeRef("b", "op2"), NodeRef("c", "op2")),
                connections = listOf(ConnectionRef("b", "out", "c", "in"))),
        )
        val outer = WorkflowDefinition(
            id = "outer", name = "Outer",
            nodes = listOf(NodeRef("upstream", "op2"), sg),
            connections = listOf(ConnectionRef("upstream", "out", "sg", "tweak")),
        )

        // Without workflow context, the optional free input is hidden and the anchor falls back to index 0.
        val hidden = resolvePortAnchor(sg, "tweak", isInput = true, optSpecs, canvasCards = null, workflow = null)
        val visible = resolvePortAnchor(sg, "in", isInput = true, optSpecs, canvasCards = null, workflow = null)
        assertEquals(visible.anchorYDp, hidden.anchorYDp, "hidden port falls back to the same row as the only visible one")

        // With the outer workflow, the connected optional port gets its own row.
        val tweakAnchor = resolvePortAnchor(sg, "tweak", isInput = true, optSpecs, canvasCards = null, workflow = outer)
        val inAnchor = resolvePortAnchor(sg, "in", isInput = true, optSpecs, canvasCards = null, workflow = outer)
        assertTrue(tweakAnchor.anchorYDp > inAnchor.anchorYDp, "connected optional port must get a distinct row")
    }

    @Test
    fun anchorsResolveByPortNameNotDeclarationOrder() {
        val node = NodeRef(id = "n", type = "op")
        val spec = specs.resolve("op")!!
        val second = resolvePortAnchor(node, "second", isInput = true, specs, canvasCards = null, workflow = null)
        val first = resolvePortAnchor(node, "first", isInput = true, specs, canvasCards = null, workflow = null)
        assertEquals(
            FieldCardFactory(inputRows = 2, outputRows = 1).portAnchorY(1, isInput = true, spec),
            second.anchorYDp,
        )
        assertEquals(true, second.anchorYDp > first.anchorYDp)
    }
}
