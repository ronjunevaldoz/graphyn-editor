package com.ronjunevaldoz.graphyn.editor.ai

import com.ronjunevaldoz.graphyn.ai.WorkflowGenerationResult
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphynAiAssistantStateTest {

    private fun state(result: WorkflowGenerationResult, applied: MutableList<WorkflowDefinition> = mutableListOf()) =
        GraphynAiAssistantState(
            generator = object : WorkflowGenerator {
                override suspend fun generate(prompt: String, catalog: List<NodeSpec>) = result
            },
            catalog = emptyList(),
            onApply = { applied.add(it) },
        )

    private val sampleWorkflow = WorkflowDefinition(
        id = "w", name = "W",
        nodes = listOf(NodeRef("a", "t"), NodeRef("b", "t")),
        connections = emptyList(),
    )

    @Test
    fun successAppliesWorkflowAndRecordsDoneTurn() = runTest {
        val applied = mutableListOf<WorkflowDefinition>()
        val s = state(WorkflowGenerationResult.Success(sampleWorkflow), applied)

        s.submit("build it")

        assertEquals(listOf(sampleWorkflow), applied)
        val turn = s.turns.single()
        assertEquals("build it", turn.prompt)
        val done = turn.status as AiTurnStatus.Done
        assertTrue(done.summary.contains("2 nodes"))
        assertNull(done.warning)
        assertEquals(false, s.generating)
    }

    @Test
    fun unsupportedNodesSurfaceAsWarning() = runTest {
        val s = state(WorkflowGenerationResult.Success(
            sampleWorkflow, droppedNodes = listOf("x (mystery)"), droppedConnections = 2,
        ))

        s.submit("build it")

        val done = s.turns.single().status as AiTurnStatus.Done
        assertTrue(done.warning!!.contains("Skipped unsupported"), "warning: ${done.warning}")
        assertTrue(done.warning!!.contains("mystery"))
        assertTrue(done.warning!!.contains("2 invalid connection"))
    }

    @Test
    fun failureRecordsErrorTurnAndAppliesNothing() = runTest {
        val applied = mutableListOf<WorkflowDefinition>()
        val s = state(WorkflowGenerationResult.Failure("host unreachable"), applied)

        s.submit("build it")

        assertTrue(applied.isEmpty())
        assertEquals("host unreachable", (s.turns.single().status as AiTurnStatus.Error).message)
    }

    @Test
    fun blankPromptIsIgnored() = runTest {
        val s = state(WorkflowGenerationResult.Success(sampleWorkflow))
        s.submit("   ")
        assertTrue(s.turns.isEmpty())
    }

    @Test
    fun clearResetsTranscript() = runTest {
        val s = state(WorkflowGenerationResult.Success(sampleWorkflow))
        s.submit("build it")
        s.clear()
        assertTrue(s.turns.isEmpty())
    }

    @Test
    fun analysisPromptSummarizesCurrentWorkflowWithoutCallingGenerator() = runTest {
        val s = GraphynAiAssistantState(
            generator = object : WorkflowGenerator {
                override suspend fun generate(prompt: String, catalog: List<NodeSpec>) =
                    error("generator should not run for local analysis")
            },
            catalog = emptyList(),
            onApply = {},
            currentWorkflow = { sampleWorkflow },
            validateWorkflow = {
                listOf(ValidationError(code = "missing_required_input", message = "Need prompt", nodeId = "a"))
            },
        )

        s.submit("analyze this workflow")

        val done = s.turns.single().status as AiTurnStatus.Done
        assertTrue(done.summary.contains("2 nodes"))
        assertTrue(done.summary.contains("Validator found 1 issue"))
        assertTrue(done.warning!!.contains("missing_required_input"))
    }
}
