package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionException
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun nodeWithoutSubgraphAndWithoutExecutorThrows() = runTest {
        val workflow = WorkflowDefinition(
            id = "broken", name = "Broken",
            nodes = listOf(NodeRef("sg", "completely.unknown.type")),
            connections = emptyList(),
        )

        assertFailsWith<WorkflowExecutionException> {
            engine.execute(workflow)
        }
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
}
