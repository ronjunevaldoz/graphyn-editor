package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StoryboardValidateTest {

    private fun sceneRecord(prompt: String?, caption: String?) = WorkflowValue.RecordValue(
        buildMap {
            if (prompt != null) put("prompt", WorkflowValue.StringValue(prompt))
            if (caption != null) put("caption", WorkflowValue.StringValue(caption))
        },
    )

    @Test
    fun malformedTopLevelThrowsInsteadOfSubstitutingUnrelatedContent() = runTest {
        // Changed 2026-07-08: used to silently fall back to a hardcoded "cooking" storyboard —
        // confirmed harmful in practice (an unreachable Ollama host produced fully-generated,
        // completely off-topic shorts with no visible error). Throwing here is strictly cheaper
        // than continuing, since it fails before any GPU spend.
        val error = assertFailsWith<IllegalStateException> {
            storyboardValidateExecutor.execute(mapOf("input" to WorkflowValue.RecordValue(emptyMap())))
        }
        assertTrue(error.message!!.contains("niche"))
        assertTrue(error.message!!.contains("scenes"))
    }

    @Test
    fun malformedTopLevelWithChainDiagnosticsSurfacesRootCause() = runTest {
        // Reproduces the real incident: an unreachable Ollama host means the top-level fields are
        // missing, but previously the only diagnostic was "Raw response: null" — the actual HTTP
        // failure was lost three layers upstream. ollamaFetchSubgraph bundles the chain's per-stage
        // signals into one "diagnostics" string (as storyboardGeneratorSubgraph now wires in), which
        // must put the root cause in the thrown message.
        val diagnostics = ollamaChainDiagnostics(
            mapOf(
                "httpOk" to WorkflowValue.BooleanValue(false),
                "httpError" to WorkflowValue.StringValue("Connection refused"),
            ),
        )
        val error = assertFailsWith<IllegalStateException> {
            storyboardValidateExecutor.execute(
                mapOf(
                    "input" to WorkflowValue.RecordValue(emptyMap()),
                    "diagnostics" to WorkflowValue.StringValue(diagnostics),
                ),
            )
        }
        assertTrue(error.message!!.contains("Connection refused"), "message was: ${error.message}")
    }

    @Test
    fun singleMalformedSceneIsSalvagedNotDropped() = runTest {
        val input = WorkflowValue.RecordValue(
            mapOf(
                "niche" to WorkflowValue.StringValue("gardening"),
                "visual_style" to WorkflowValue.StringValue("flat vector illustration"),
                "character" to WorkflowValue.StringValue("a gardener in overalls"),
                "narration" to WorkflowValue.StringValue("Grow your own herbs at home."),
                "scenes" to WorkflowValue.ListValue(
                    listOf(
                        sceneRecord("a sunny windowsill herb garden", "Start small."),
                        sceneRecord(null, null), // dropped prompt on exactly one scene
                        sceneRecord("fresh basil in a pot", "Snip and enjoy."),
                    ),
                ),
            ),
        )
        val record = storyboardValidateExecutor.execute(mapOf("input" to input))["value"] as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.StringValue("gardening"), record.fields["niche"])
        val scenes = (record.fields["scenes"] as WorkflowValue.ListValue).items
        assertEquals(STORYBOARD_SCENE_COUNT, scenes.size)
        val patched = (scenes[1] as WorkflowValue.RecordValue).fields["prompt"] as WorkflowValue.StringValue
        assertTrue(patched.value.isNotBlank())
    }
}
