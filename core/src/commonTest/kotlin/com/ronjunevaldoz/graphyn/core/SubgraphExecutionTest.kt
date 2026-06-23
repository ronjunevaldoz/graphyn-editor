package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SubgraphExecutionTest {

    private val executors = DefaultNodeExecutorRegistry().apply {
        register("passthrough") { inputs ->
            mapOf("out" to (inputs["in"] ?: WorkflowValue.NullValue))
        }
        register("double") { inputs ->
            val n = (inputs["n"] as? WorkflowValue.IntValue)?.value ?: 0
            mapOf("result" to WorkflowValue.IntValue(n * 2))
        }
    }
    private val engine = WorkflowExecutionEngine(executors)

    private fun innerWorkflow(seedValue: Int = 42) = WorkflowDefinition(
        id = "inner", name = "Inner",
        nodes = listOf(NodeRef("p1", "passthrough", config = mapOf("in" to WorkflowValue.IntValue(seedValue)))),
        connections = emptyList(),
    )

    @Test
    fun subgraphNodeRunsInnerWorkflowAndPropagatesOutputsDownstream() = runTest {
        val workflow = WorkflowDefinition(
            id = "outer", name = "Outer",
            nodes = listOf(
                NodeRef("sg", "any.subgraph", subgraph = innerWorkflow(seedValue = 42)),
                NodeRef("d1", "double"),
            ),
            connections = listOf(ConnectionRef("sg", "out", "d1", "n")),
        )

        val result = engine.execute(workflow)

        // inner p1 outputs {out=42}; sg inherits that; d1 doubles to 84
        assertEquals(WorkflowValue.IntValue(84), result.nodeOutputsByNodeId["d1"]?.get("result"))
        assertEquals(listOf("sg", "d1"), result.executionOrder)
    }

    @Test
    fun subgraphNodeRequiresNoRegisteredExecutor() = runTest {
        val workflow = WorkflowDefinition(
            id = "outer2", name = "Outer",
            nodes = listOf(NodeRef("sg", "completely.unknown.type", subgraph = innerWorkflow())),
            connections = emptyList(),
        )

        val result = engine.execute(workflow)
        assertEquals(listOf("sg"), result.executionOrder)
    }

    @Test
    fun nodeWithoutSubgraphAndWithoutExecutorIsRecordedAsError() = runTest {
        // Resilient execution: a missing executor fails only that node, it does not abort the run.
        val workflow = WorkflowDefinition(
            id = "broken", name = "Broken",
            nodes = listOf(NodeRef("sg", "completely.unknown.type")),
            connections = emptyList(),
        )

        val result = engine.execute(workflow)
        assertEquals(NodeExecutionStatus.Error, result.statusByNodeId["sg"])
        assertTrue(result.errorsByNodeId["sg"]?.contains("No executor registered") == true)
    }

    @Test
    fun nestedSubgraphsExecuteRecursively() = runTest {
        val innermost = WorkflowDefinition(
            id = "innermost", name = "Innermost",
            nodes = listOf(NodeRef("p", "passthrough", config = mapOf("in" to WorkflowValue.IntValue(7)))),
            connections = emptyList(),
        )
        val middle = WorkflowDefinition(
            id = "middle", name = "Middle",
            nodes = listOf(NodeRef("sg", "wrap", subgraph = innermost)),
            connections = emptyList(),
        )
        val outer = WorkflowDefinition(
            id = "outer3", name = "Outer",
            nodes = listOf(
                NodeRef("sg", "wrap", subgraph = middle),
                NodeRef("d1", "double"),
            ),
            connections = listOf(ConnectionRef("sg", "out", "d1", "n")),
        )

        val result = engine.execute(outer)
        // Innermost outputs out=7; middle inherits it; outer's double produces 14
        assertEquals(WorkflowValue.IntValue(14), result.nodeOutputsByNodeId["d1"]?.get("result"))
    }

    @Test
    fun subgraphWithRegisteredExecutorMapsOutputsThroughExecutor() = runTest {
        // Executor remaps inner output key "out" → declared port "result"
        val executors2 = DefaultNodeExecutorRegistry().apply {
            register("passthrough") { inputs ->
                mapOf("out" to (inputs["in"] ?: WorkflowValue.NullValue))
            }
            register("mapped.subgraph") { inputs ->
                mapOf("result" to (inputs["out"] ?: WorkflowValue.NullValue))
            }
        }
        val inner = WorkflowDefinition(
            id = "inner-mapped", name = "Inner",
            nodes = listOf(NodeRef("p", "passthrough", config = mapOf("in" to WorkflowValue.IntValue(99)))),
            connections = emptyList(),
        )
        val workflow = WorkflowDefinition(
            id = "outer-mapped", name = "Outer",
            nodes = listOf(NodeRef("sg", "mapped.subgraph", subgraph = inner)),
            connections = emptyList(),
        )

        val result = WorkflowExecutionEngine(executors2).execute(workflow)
        // Executor must be called; inner raw key "out" → declared key "result"
        assertEquals(WorkflowValue.IntValue(99), result.nodeOutputsByNodeId["sg"]?.get("result"),
            "executor should remap inner output 'out' to declared port 'result'")
        assertEquals(null, result.nodeOutputsByNodeId["sg"]?.get("out"),
            "raw inner key must not leak through when executor is present")
    }

    @Test
    fun subgraphInputInjectionFeedsInnerFreePorts() = runTest {
        // Inner "p1" has a free "in" port (no config, no internal connection) — it must
        // pull from the subgraph node's incoming "in" value supplied by an upstream node.
        val inner = WorkflowDefinition(
            id = "inner-injected", name = "Inner",
            nodes = listOf(NodeRef("p1", "passthrough")),
            connections = emptyList(),
        )
        val workflow = WorkflowDefinition(
            id = "outer-injected", name = "Outer",
            nodes = listOf(
                NodeRef("seed", "passthrough", config = mapOf("in" to WorkflowValue.IntValue(21))),
                NodeRef("sg", "any.subgraph", subgraph = inner),
                NodeRef("d1", "double"),
            ),
            connections = listOf(
                ConnectionRef("seed", "out", "sg", "in"),   // feeds the subgraph node's "in" port
                ConnectionRef("sg", "out", "d1", "n"),
            ),
        )

        val result = engine.execute(workflow)

        // seed=21 → sg.in=21 → injected into inner p1.in → inner out=21 → sg out=21 → double=42
        assertEquals(WorkflowValue.IntValue(42), result.nodeOutputsByNodeId["d1"]?.get("result"),
            "subgraph input must flow into the inner workflow's free port")
    }

    @Test
    fun innerConfigWinsOverInjectedInput() = runTest {
        // Inner "p1" sets in=5 via config; an injected subgraph input must NOT override explicit config.
        val inner = WorkflowDefinition(
            id = "inner-config", name = "Inner",
            nodes = listOf(NodeRef("p1", "passthrough", config = mapOf("in" to WorkflowValue.IntValue(5)))),
            connections = emptyList(),
        )
        val workflow = WorkflowDefinition(
            id = "outer-config", name = "Outer",
            nodes = listOf(
                NodeRef("seed", "passthrough", config = mapOf("in" to WorkflowValue.IntValue(99))),
                NodeRef("sg", "any.subgraph", subgraph = inner),
            ),
            connections = listOf(ConnectionRef("seed", "out", "sg", "in")),
        )

        val result = engine.execute(workflow)
        // inner config in=5 wins over injected 99
        assertEquals(WorkflowValue.IntValue(5), result.nodeOutputsByNodeId["sg"]?.get("out"))
    }
}
