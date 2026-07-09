package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OllamaChainDiagnosticsTest {

    @Test
    fun noWiredPortsReportsUnavailable() {
        assertEquals("no chain diagnostics wired", ollamaChainDiagnostics(emptyMap()))
    }

    @Test
    fun allStagesOkReportsOkForEach() {
        val inputs = mapOf(
            "httpOk" to WorkflowValue.BooleanValue(true),
            "outerParseOk" to WorkflowValue.BooleanValue(true),
            "responseFound" to WorkflowValue.BooleanValue(true),
            "innerParseOk" to WorkflowValue.BooleanValue(true),
        )
        val summary = ollamaChainDiagnostics(inputs)
        assertEquals("HTTP request: ok; Outer JSON parse: ok; Path 'response': ok; Inner JSON parse: ok", summary)
    }

    @Test
    fun httpFailureSurfacesActualExceptionMessageFirst() {
        // Reproduces the real-world case: the HTTP call itself failed, so every downstream stage
        // also failed, but the root cause is the HTTP error message, not the final null.
        val inputs = mapOf(
            "httpOk" to WorkflowValue.BooleanValue(false),
            "httpError" to WorkflowValue.StringValue("Connection refused"),
            "outerParseOk" to WorkflowValue.BooleanValue(false),
            "outerParseError" to WorkflowValue.StringValue("Unexpected end of input; input: <empty>"),
            "responseFound" to WorkflowValue.BooleanValue(false),
            "innerParseOk" to WorkflowValue.BooleanValue(false),
            "innerParseError" to WorkflowValue.StringValue("Unexpected end of input; input: <empty>"),
        )
        val summary = ollamaChainDiagnostics(inputs)
        assertTrue(summary.contains("HTTP request: FAILED (Connection refused)"))
        assertTrue(summary.startsWith("HTTP request:"), "root cause should be reported first in the chain")
    }

    @Test
    fun partiallyWiredInputsOmitUnwiredStages() {
        val summary = ollamaChainDiagnostics(mapOf("httpOk" to WorkflowValue.BooleanValue(true)))
        assertEquals("HTTP request: ok", summary)
    }
}
