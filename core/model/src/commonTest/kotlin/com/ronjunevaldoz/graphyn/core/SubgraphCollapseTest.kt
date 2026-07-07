package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.collapseToSubgraph
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.model.expandSubgraph
import com.ronjunevaldoz.graphyn.core.model.subgraphBoundary
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubgraphCollapseTest {

    // a -> b -> c -> d ; collapse {b, c}
    private val specs = DefaultNodeSpecRegistry().apply {
        register(NodeSpec("op", "Op",
            inputs = listOf(PortSpec("in", WorkflowType.StringType)),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))))
    }
    private val chain = WorkflowDefinition(
        id = "wf", name = "Chain",
        nodes = listOf(NodeRef("a", "op"), NodeRef("b", "op"), NodeRef("c", "op"), NodeRef("d", "op")),
        connections = listOf(
            ConnectionRef("a", "out", "b", "in"),
            ConnectionRef("b", "out", "c", "in"),
            ConnectionRef("c", "out", "d", "in"),
        ),
    )

    @Test
    fun collapseMovesInternalEdgesInsideAndRewiresBoundary() {
        val result = collapseToSubgraph(chain, setOf("b", "c"), "sg", "graphyn.subgraph")!!
        val wf = result.workflow

        // b, c removed from top level; sg added
        assertTrue(wf.nodes.none { it.id == "b" || it.id == "c" })
        val sg = wf.nodes.first { it.id == "sg" }
        assertEquals(2, sg.subgraph?.nodes?.size)
        // internal b->c moved inside
        assertEquals(1, sg.subgraph?.connections?.size)

        // a->b becomes a->sg:in ; c->d becomes sg:out->d
        assertTrue(wf.connections.any { it.fromNodeId == "a" && it.toNodeId == "sg" && it.toPort == "in" })
        assertTrue(wf.connections.any { it.fromNodeId == "sg" && it.fromPort == "out" && it.toNodeId == "d" })
    }

    @Test
    fun derivedSpecExposesBoundaryPorts() {
        val result = collapseToSubgraph(chain, setOf("b", "c"), "sg", "graphyn.subgraph")!!
        val sg = result.workflow.nodes.first { it.id == "sg" }
        val derived = deriveSubgraphSpec(sg, specs)!!
        // b.in is free (a->b now external), c.out is free (c->d now external); b.out & c.in absorbed
        assertEquals(listOf("in"), derived.inputs.map { it.name })
        assertEquals(listOf("out"), derived.outputs.map { it.name })
    }

    @Test
    fun expandIsInverseOfCollapse() {
        val collapsed = collapseToSubgraph(chain, setOf("b", "c"), "sg", "graphyn.subgraph")!!.workflow
        val expanded = expandSubgraph(collapsed, "sg", specs)!!

        assertEquals(setOf("a", "b", "c", "d"), expanded.nodes.mapTo(mutableSetOf()) { it.id })
        // original edges restored
        assertTrue(expanded.connections.any { it.fromNodeId == "a" && it.toNodeId == "b" && it.toPort == "in" })
        assertTrue(expanded.connections.any { it.fromNodeId == "b" && it.toNodeId == "c" })
        assertTrue(expanded.connections.any { it.fromNodeId == "c" && it.toNodeId == "d" })
    }

    @Test
    fun collapseRejectsFewerThanTwoNodes() {
        assertNull(collapseToSubgraph(chain, setOf("b"), "sg", "graphyn.subgraph"))
        assertNull(collapseToSubgraph(chain, emptySet(), "sg", "graphyn.subgraph"))
    }

    @Test
    fun derivedSpecHidesOptionalFreeInputsButBoundaryKeepsThem() {
        val optSpecs = DefaultNodeSpecRegistry().apply {
            register(NodeSpec("op2", "Op2",
                inputs = listOf(
                    PortSpec("in", WorkflowType.StringType),
                    PortSpec("tweak", WorkflowType.StringType, required = false),
                ),
                outputs = listOf(PortSpec("out", WorkflowType.StringType))))
        }
        val node = NodeRef(
            id = "sg", type = "graphyn.subgraph",
            subgraph = WorkflowDefinition("inner", "Inner",
                nodes = listOf(NodeRef("b", "op2"), NodeRef("c", "op2")),
                connections = listOf(ConnectionRef("b", "out", "c", "in"))),
        )

        // The display spec hides the optional free input…
        val derived = deriveSubgraphSpec(node, optSpecs)!!
        assertEquals(listOf("in"), derived.inputs.map { it.name })
        // …but the execution/rewiring boundary still exposes it.
        val boundary = subgraphBoundary(node.subgraph!!, optSpecs)
        assertEquals(listOf("in", "tweak"), boundary.inputs.map { it.name })
    }

    @Test
    fun boundaryDedupesSharedPortNames() {
        val boundary = subgraphBoundary(
            WorkflowDefinition("inner", "Inner",
                nodes = listOf(NodeRef("b", "op"), NodeRef("c", "op")),
                connections = emptyList()),
            specs,
        )
        // both b and c expose free "in"/"out" but names dedupe to one each
        assertEquals(listOf("in"), boundary.inputs.map { it.name })
        assertEquals(listOf("out"), boundary.outputs.map { it.name })
    }
}
