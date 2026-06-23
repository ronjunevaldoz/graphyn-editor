package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceholderWorkflowGeneratorTest {

    private val catalog = listOf(
        NodeSpec("a", "A", inputs = emptyList(), outputs = listOf(PortSpec("out", WorkflowType.StringType))),
        NodeSpec("b", "B", inputs = listOf(PortSpec("in", WorkflowType.StringType)),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))),
        NodeSpec("c", "C", inputs = listOf(PortSpec("in", WorkflowType.StringType)), outputs = emptyList()),
    )

    @Test
    fun buildsLinearChainFromCatalog() = runTest {
        val gen = PlaceholderWorkflowGenerator(delayMs = 0)
        val result = gen.generate("anything", catalog) as WorkflowGenerationResult.Success
        assertEquals(3, result.workflow.nodes.size)
        assertEquals(2, result.workflow.connections.size)
        assertEquals("anything", result.workflow.name)
    }

    @Test
    fun failsOnEmptyCatalog() = runTest {
        val gen = PlaceholderWorkflowGenerator(delayMs = 0)
        assertTrue(gen.generate("x", emptyList()) is WorkflowGenerationResult.Failure)
    }
}
