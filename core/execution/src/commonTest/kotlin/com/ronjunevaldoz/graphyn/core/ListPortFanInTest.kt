package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/** Multiple connections into one `ListType` input port collapse into a single [WorkflowValue.ListValue]. */
class ListPortFanInTest {
    private val specs = DefaultNodeSpecRegistry().apply {
        register(NodeSpec("source", "Source",
            inputs = emptyList(),
            outputs = listOf(PortSpec("out", WorkflowType.IntType))))
        register(NodeSpec("collector", "Collector",
            inputs = listOf(PortSpec("items", WorkflowType.ListType(WorkflowType.IntType))),
            outputs = listOf(PortSpec("count", WorkflowType.IntType))))
    }

    private fun engineWith(vararg pairs: Pair<String, (Map<String, WorkflowValue>) -> Map<String, WorkflowValue>>) =
        WorkflowExecutionEngine(DefaultNodeExecutorRegistry().apply {
            pairs.forEach { (t, fn) -> register(t, fn) }
        }, specs)

    @Test
    fun multipleConnectionsCollectIntoListValue() = runTest {
        val eng = engineWith(
            "source" to { inputs -> mapOf("out" to (inputs["seed"] ?: intOf(0))) },
            "collector" to { inputs ->
                val items = (inputs["items"] as? WorkflowValue.ListValue)?.items ?: emptyList()
                mapOf("count" to intOf(items.size))
            },
        )
        val result = eng.execute(wf(
            NodeRef("a", "source", config = mapOf("seed" to intOf(1))),
            NodeRef("b", "source", config = mapOf("seed" to intOf(2))),
            NodeRef("c", "source", config = mapOf("seed" to intOf(3))),
            NodeRef("sink", "collector"),
            connections = listOf(
                conn("a", "out", "sink", "items"),
                conn("b", "out", "sink", "items"),
                conn("c", "out", "sink", "items"),
            ),
        ))
        assertEquals(intOf(3), result.nodeOutputsByNodeId["sink"]?.get("count"))
    }

    @Test
    fun singleConnectionStillWrapsIntoList() = runTest {
        val eng = engineWith(
            "source" to { _ -> mapOf("out" to intOf(7)) },
            "collector" to { inputs ->
                val items = (inputs["items"] as? WorkflowValue.ListValue)?.items ?: emptyList()
                mapOf("count" to intOf(items.size))
            },
        )
        val result = eng.execute(wf(
            NodeRef("a", "source"),
            NodeRef("sink", "collector"),
            connections = listOf(conn("a", "out", "sink", "items")),
        ))
        assertEquals(intOf(1), result.nodeOutputsByNodeId["sink"]?.get("count"))
    }
}
