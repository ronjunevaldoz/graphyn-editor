package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeConfigPropagationTest {

    @Test
    fun nestedChainABCPropagatesConfigValue() {
        // A(x=3) -> B(double) -> C(+1) → expect 7
        val eng = engine(
            "a" to { inputs -> mapOf("out" to (inputs["x"] ?: intOf(0))) },
            "b" to { inputs -> mapOf("out" to intOf(((inputs["in"] as? WorkflowValue.IntValue)?.value ?: 0) * 2)) },
            "c" to { inputs -> mapOf("out" to intOf(((inputs["in"] as? WorkflowValue.IntValue)?.value ?: 0) + 1)) },
        )
        val result = eng.execute(wf(
            NodeRef("a", "a", config = mapOf("x" to intOf(3))),
            NodeRef("b", "b"),
            NodeRef("c", "c"),
            connections = listOf(conn("a", "out", "b", "in"), conn("b", "out", "c", "in")),
        ))
        assertEquals(intOf(7), result.nodeOutputsByNodeId["c"]?.get("out"))
    }

    @Test
    fun deepChainFourHopsPropagatesValue() {
        val eng = engine(
            "src" to { inputs -> mapOf("v" to (inputs["v"] ?: intOf(0))) },
            "hop" to { inputs -> mapOf("v" to (inputs["v"] ?: intOf(0))) },
        )
        val result = eng.execute(wf(
            NodeRef("a", "src", config = mapOf("v" to intOf(8))),
            NodeRef("b", "hop"), NodeRef("c", "hop"), NodeRef("d", "hop"),
            connections = listOf(
                conn("a", "v", "b", "v"), conn("b", "v", "c", "v"), conn("c", "v", "d", "v"),
            ),
        ))
        assertEquals(intOf(8), result.nodeOutputsByNodeId["d"]?.get("v"))
    }

    @Test
    fun fanOutBothDownstreamNodesReflectSourceConfig() {
        val eng = engine(
            "src" to { inputs -> mapOf("val" to (inputs["v"] ?: intOf(0))) },
            "b"   to { inputs -> inputs },
            "c"   to { inputs -> inputs },
        )
        val result = eng.execute(wf(
            NodeRef("src", "src", config = mapOf("v" to intOf(5))),
            NodeRef("b", "b"), NodeRef("c", "c"),
            connections = listOf(conn("src", "val", "b", "val"), conn("src", "val", "c", "val")),
        ))
        assertEquals(intOf(5), result.nodeOutputsByNodeId["b"]?.get("val"))
        assertEquals(intOf(5), result.nodeOutputsByNodeId["c"]?.get("val"))
    }

    @Test
    fun fanInSumsInputsFromTwoUpstreamNodes() {
        val eng = engine(
            "a"   to { inputs -> mapOf("out" to (inputs["v"] ?: intOf(0))) },
            "b"   to { inputs -> mapOf("out" to (inputs["v"] ?: intOf(0))) },
            "sum" to { inputs ->
                val x = (inputs["x"] as? WorkflowValue.IntValue)?.value ?: 0
                val y = (inputs["y"] as? WorkflowValue.IntValue)?.value ?: 0
                mapOf("total" to intOf(x + y))
            },
        )
        val result = eng.execute(wf(
            NodeRef("a", "a", config = mapOf("v" to intOf(3))),
            NodeRef("b", "b", config = mapOf("v" to intOf(4))),
            NodeRef("sum", "sum"),
            connections = listOf(conn("a", "out", "sum", "x"), conn("b", "out", "sum", "y")),
        ))
        assertEquals(intOf(7), result.nodeOutputsByNodeId["sum"]?.get("total"))
    }

    @Test
    fun diamondTopologyBothPathsMergeAtSink() {
        // source → left and right separately → sink sums both
        val eng = engine(
            "src"  to { inputs -> mapOf("v" to (inputs["v"] ?: intOf(0))) },
            "left" to { inputs -> mapOf("out" to intOf(((inputs["v"] as? WorkflowValue.IntValue)?.value ?: 0) + 1)) },
            "right" to { inputs -> mapOf("out" to intOf(((inputs["v"] as? WorkflowValue.IntValue)?.value ?: 0) * 2)) },
            "sink" to { inputs ->
                val l = (inputs["l"] as? WorkflowValue.IntValue)?.value ?: 0
                val r = (inputs["r"] as? WorkflowValue.IntValue)?.value ?: 0
                mapOf("total" to intOf(l + r))
            },
        )
        val result = eng.execute(wf(
            NodeRef("src", "src", config = mapOf("v" to intOf(3))),
            NodeRef("l", "left"), NodeRef("r", "right"), NodeRef("sink", "sink"),
            connections = listOf(
                conn("src", "v", "l", "v"), conn("src", "v", "r", "v"),
                conn("l", "out", "sink", "l"), conn("r", "out", "sink", "r"),
            ),
        ))
        // src=3 → left=4, right=6 → sink=10
        assertEquals(intOf(10), result.nodeOutputsByNodeId["sink"]?.get("total"))
    }

    @Test
    fun configChangeOnIsolatedNodeDoesNotAffectOther() {
        val eng = engine("node" to { inputs -> mapOf("out" to (inputs["v"] ?: intOf(0))) })
        val result = eng.execute(wf(
            NodeRef("a", "node", config = mapOf("v" to intOf(99))),
            NodeRef("b", "node", config = mapOf("v" to intOf(7))),
        ))
        assertEquals(intOf(99), result.nodeOutputsByNodeId["a"]?.get("out"))
        assertEquals(intOf(7),  result.nodeOutputsByNodeId["b"]?.get("out"))
    }
}
