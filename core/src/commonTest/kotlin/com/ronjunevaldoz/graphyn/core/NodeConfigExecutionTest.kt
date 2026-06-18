package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

internal fun intOf(n: Int) = WorkflowValue.IntValue(n)
internal fun strOf(s: String) = WorkflowValue.StringValue(s)
internal fun conn(a: String, ap: String, b: String, bp: String) = ConnectionRef(a, ap, b, bp)

internal fun engine(vararg pairs: Pair<String, (Map<String, WorkflowValue>) -> Map<String, WorkflowValue>>) =
    WorkflowExecutionEngine(DefaultNodeExecutorRegistry().apply {
        pairs.forEach { (t, fn) -> register(t, fn) }
    })

internal fun wf(vararg nodes: NodeRef, connections: List<ConnectionRef> = emptyList()) =
    WorkflowDefinition("wf", "W", nodes.toList(), connections)

class NodeConfigExecutionTest {

    @Test
    fun configOverridesSpecDefault() {
        val specs = DefaultNodeSpecRegistry().apply {
            register(NodeSpec("echo", "Echo",
                inputs = listOf(PortSpec("count", WorkflowType.IntType)),
                outputs = listOf(PortSpec("out", WorkflowType.IntType)),
                defaultValues = mapOf("count" to intOf(5)),
            ))
        }
        val eng = WorkflowExecutionEngine(DefaultNodeExecutorRegistry().apply {
            register("echo") { inputs -> mapOf("out" to (inputs["count"] ?: intOf(0))) }
        }, specs)
        val result = eng.execute(wf(NodeRef("n1", "echo", config = mapOf("count" to intOf(10)))))
        assertEquals(intOf(10), result.nodeOutputsByNodeId["n1"]?.get("out"))
    }

    @Test
    fun specDefaultUsedWhenConfigAbsent() {
        val specs = DefaultNodeSpecRegistry().apply {
            register(NodeSpec("echo", "Echo",
                inputs = listOf(PortSpec("count", WorkflowType.IntType)),
                outputs = listOf(PortSpec("out", WorkflowType.IntType)),
                defaultValues = mapOf("count" to intOf(5)),
            ))
        }
        val eng = WorkflowExecutionEngine(DefaultNodeExecutorRegistry().apply {
            register("echo") { inputs -> mapOf("out" to (inputs["count"] ?: intOf(0))) }
        }, specs)
        assertEquals(intOf(5), eng.execute(wf(NodeRef("n1", "echo"))).nodeOutputsByNodeId["n1"]?.get("out"))
    }

    @Test
    fun connectedInputOverridesNodeConfig() {
        val eng = engine(
            "source" to { _ -> mapOf("val" to intOf(42)) },
            "sink"   to { inputs -> mapOf("out" to (inputs["val"] ?: intOf(0))) },
        )
        val result = eng.execute(wf(
            NodeRef("src", "source"),
            NodeRef("snk", "sink", config = mapOf("val" to intOf(99))),
            connections = listOf(conn("src", "val", "snk", "val")),
        ))
        assertEquals(intOf(42), result.nodeOutputsByNodeId["snk"]?.get("out"))
    }

    @Test
    fun enumConfigValueBranchesExecutorLogic() {
        val eng = engine("node" to { inputs ->
            val mode = (inputs["mode"] as? WorkflowValue.StringValue)?.value
            mapOf("result" to intOf(if (mode == "fast") 1 else 2))
        })
        fun run(mode: String) = eng.execute(
            wf(NodeRef("n", "node", config = mapOf("mode" to strOf(mode))))
        ).nodeOutputsByNodeId["n"]?.get("result")

        assertEquals(intOf(1), run("fast"))
        assertEquals(intOf(2), run("quality"))
    }

    @Test
    fun multiEnumListValueCountedByExecutor() {
        val eng = engine("node" to { inputs ->
            val list = (inputs["channels"] as? WorkflowValue.ListValue)?.items ?: emptyList()
            mapOf("count" to intOf(list.size))
        })
        val result = eng.execute(wf(NodeRef("n", "node", config = mapOf(
            "channels" to WorkflowValue.ListValue(listOf(strOf("R"), strOf("G"), strOf("B"))),
        ))))
        assertEquals(intOf(3), result.nodeOutputsByNodeId["n"]?.get("count"))
    }
}
