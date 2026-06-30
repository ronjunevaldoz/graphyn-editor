@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The SD config nodes expose many nullable-but-required ports, so every SD workflow (including the
 * shipped FLUX one) reports `missing_required_input` warnings plus a tolerated `type_mismatch` on
 * the preview's opaque `value` port. These are non-blocking. This test instead asserts the new
 * LoRA workflows are *structurally* sound: no duplicate input connections, and no type mismatch
 * outside the tolerated preview sink — i.e. the LoRA wiring isn't silently broken.
 */
class SdLoraWorkflowValidationTest {
    private val registry = DefaultGraphynPluginRegistry().apply {
        (GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
    }
    private val validator = WorkflowGraphValidator(registry.nodeSpecs)

    private fun assertStructurallySound(wf: WorkflowDefinition) {
        val errors: List<ValidationError> = validator.validate(wf)
        assertEquals(
            emptyList(),
            errors.filter { it.code == "duplicate_input_connection" },
            "no input port may have multiple connections",
        )
        val badMismatch = errors.filter { it.code == "type_mismatch" && it.port != "value" }
        assertTrue(badMismatch.isEmpty(), "unexpected type mismatches: $badMismatch")
    }

    @Test fun qwenTxt2ImgIsSound() = assertStructurallySound(qwenTxt2ImgWorkflow)
    @Test fun qwenImg2ImgIsSound() = assertStructurallySound(qwenImg2ImgWorkflow)
    @Test fun wanImg2VidIsSound() = assertStructurallySound(wanImg2VidWorkflow)
}
