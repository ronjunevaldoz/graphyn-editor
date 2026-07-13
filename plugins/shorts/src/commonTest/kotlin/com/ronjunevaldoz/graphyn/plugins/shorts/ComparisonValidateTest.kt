package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComparisonValidateTest {

    @Test
    fun malformedTopLevelThrowsInsteadOfSubstitutingUnrelatedContent() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            comparisonValidateExecutor.execute(mapOf("input" to WorkflowValue.RecordValue(emptyMap())))
        }
        assertTrue(error.message!!.contains("niche"))
        assertTrue(error.message!!.contains("pairs"))
    }

    @Test
    fun doesNotRequireNarrationFromTheModel() = runTest {
        // buildComparisonPrompt no longer asks for it (narration is rebuilt from the pairs below),
        // so a response missing it entirely must not be treated as malformed.
        val error = assertFailsWith<IllegalStateException> {
            comparisonValidateExecutor.execute(mapOf("input" to WorkflowValue.RecordValue(emptyMap())))
        }
        assertTrue(!error.message!!.contains("narration"), "message was: ${error.message}")
    }

    private fun pair(labelA: String?, labelB: String?, question: String?, answer: String?) =
        WorkflowValue.RecordValue(
            buildMap {
                if (labelA != null) put("label_a", WorkflowValue.StringValue(labelA))
                if (labelB != null) put("label_b", WorkflowValue.StringValue(labelB))
                if (question != null) put("question", WorkflowValue.StringValue(question))
                if (answer != null) put("answer", WorkflowValue.StringValue(answer))
            },
        )

    @Test
    fun narrationIsBuiltFromPairsNotTrustedFromTheModel() = runTest {
        // Reproduces the real incident: the model's own narration prose described 7 comparisons
        // while only 4 structured pairs existed, so audio and on-screen pairs drifted out of sync.
        // narration must now be derived only from the validated pairs, in pair order.
        val input = WorkflowValue.RecordValue(
            mapOf(
                "niche" to WorkflowValue.StringValue("coffee"),
                "visual_style" to WorkflowValue.StringValue("flat vector"),
                "narration" to WorkflowValue.StringValue(
                    "This model-written prose mentions six different unrelated comparisons and must be ignored.",
                ),
                "pairs" to WorkflowValue.ListValue(
                    List(COMPARISON_PAIR_COUNT) { pair("A$it", "B$it", "Q$it?", "A$it wins.") },
                ),
            ),
        )
        val result = comparisonValidateExecutor.execute(mapOf("input" to input))
        val narration = ((result.getValue("value") as WorkflowValue.RecordValue).fields.getValue("narration") as WorkflowValue.StringValue).value
        assertEquals("Q0? A0 wins. Q1? A1 wins. Q2? A2 wins. Q3? A3 wins.", narration)
    }

    @Test
    fun blankStringFieldsSalvageTheSameAsMissingFields() = runTest {
        // Reproduces the real incident: Ollama returned label_a/label_b missing (correctly
        // salvaged) but question/answer present as "" — a blank string, not a missing key — which
        // the old `?: fallback` on the raw nullable never caught, leaving a silently blank caption.
        val input = WorkflowValue.RecordValue(
            mapOf(
                "niche" to WorkflowValue.StringValue("coffee"),
                "visual_style" to WorkflowValue.StringValue("flat vector"),
                "narration" to WorkflowValue.StringValue("ignored"),
                "pairs" to WorkflowValue.ListValue(
                    listOf(pair(labelA = null, labelB = null, question = "", answer = "")) +
                        List(COMPARISON_PAIR_COUNT - 1) { pair("A", "B", "Q?", "A wins.") },
                ),
            ),
        )
        val result = comparisonValidateExecutor.execute(mapOf("input" to input))
        val pairs = ((result.getValue("value") as WorkflowValue.RecordValue).fields.getValue("pairs") as WorkflowValue.ListValue).items
        val firstPair = (pairs.first() as WorkflowValue.RecordValue).fields
        assertEquals(WorkflowValue.StringValue("Thing A"), firstPair["label_a"])
        assertEquals(WorkflowValue.StringValue("Thing B"), firstPair["label_b"])
        assertEquals(WorkflowValue.StringValue("What's the difference?"), firstPair["question"])
        assertEquals(WorkflowValue.StringValue("Thing A and Thing B differ."), firstPair["answer"])
    }

    @Test
    fun malformedTopLevelWithChainDiagnosticsSurfacesRootCause() = runTest {
        // Sibling to StoryboardValidateTest's equivalent case — comparisonGeneratorSubgraph's
        // ollamaFetchSubgraph bundles chain diagnostics into one string before wiring it into
        // demo.comparison.validate's "diagnostics" port.
        val diagnostics = ollamaChainDiagnostics(
            mapOf(
                "innerParseOk" to WorkflowValue.BooleanValue(false),
                "innerParseError" to WorkflowValue.StringValue("Unexpected token at offset 0"),
            ),
        )
        val error = assertFailsWith<IllegalStateException> {
            comparisonValidateExecutor.execute(
                mapOf(
                    "input" to WorkflowValue.RecordValue(emptyMap()),
                    "diagnostics" to WorkflowValue.StringValue(diagnostics),
                ),
            )
        }
        assertTrue(error.message!!.contains("Unexpected token at offset 0"), "message was: ${error.message}")
    }
}
