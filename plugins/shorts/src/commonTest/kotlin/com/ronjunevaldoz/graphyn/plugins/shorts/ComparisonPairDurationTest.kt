package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ComparisonPairDurationTest {

    private suspend fun perPairMs(narrationDurationMs: Double, pairCount: Int? = null): Double {
        val inputs = buildMap {
            put("narration_duration_ms", WorkflowValue.DoubleValue(narrationDurationMs))
            if (pairCount != null) put("pair_count", WorkflowValue.IntValue(pairCount))
        }
        val result = comparisonPairDurationExecutor.execute(inputs)
        return (result.getValue("result") as WorkflowValue.DoubleValue).value
    }

    @Test
    fun dividesNarrationEvenlyAcrossPairsWithinBounds() = runTest {
        // 16000ms narration / 4 pairs = 4000ms, comfortably inside the 2000-8000ms band.
        assertEquals(4000.0, perPairMs(16000.0, pairCount = 4))
    }

    @Test
    fun ceilingCapsRunawayTtsDurationAnomaly() = runTest {
        // Reproduces the real incident: media.text_to_speech.qwen3 reported ~163800ms for a
        // narration that should take ~30-40s to speak (confirmed via direct audio inspection to
        // contain 40+ second internal silence gaps — a TTS synthesis issue, not something this node
        // can fix). Without the ceiling, 163800/4 = 40950ms/pair produced a 163.8s "short."
        assertEquals(8000.0, perPairMs(163_800.0, pairCount = 4))
    }

    @Test
    fun floorKeepsVeryShortNarrationWatchable() = runTest {
        // A near-silent/very short narration must not collapse pairs to a sub-second flash.
        assertEquals(2000.0, perPairMs(400.0, pairCount = 4))
    }

    @Test
    fun missingNarrationDurationFallsBackToFloor() = runTest {
        assertEquals(2000.0, perPairMs(narrationDurationMs = 0.0))
    }
}
