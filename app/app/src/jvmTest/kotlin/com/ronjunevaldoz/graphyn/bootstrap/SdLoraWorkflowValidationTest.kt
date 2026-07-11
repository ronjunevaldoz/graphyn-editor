@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.workflows.*
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
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
    private companion object {
        const val QWEN_TXT2IMG_DIFFUSION = "/models/qwen/diffusion/qwen-image-2512-Q2_K.gguf"
        const val QWEN_IMG2IMG_DIFFUSION = "/models/qwen/diffusion/qwen-image-edit-2511-Q2_K.gguf"
        const val QWEN_TXT2IMG_LORA = "/models/qwen/lora/Qwen-Image-2512-Lightning-4steps-V1.0-fp32.safetensors"
        const val QWEN_IMG2IMG_LORA = "/models/qwen/lora/Qwen-Image-Edit-2511-Lightning-4steps-V1.0-fp32.safetensors"
    }

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

    private fun assertNodePath(wf: WorkflowDefinition, nodeId: String, expected: String) {
        val node = wf.nodes.firstOrNull { it.id == nodeId }
        val actual = (node?.config?.get("path") as? WorkflowValue.StringValue)?.value
        assertEquals(expected, actual, "unexpected LoRA path on node '$nodeId'")
    }

    private fun assertStringConfig(wf: WorkflowDefinition, nodeId: String, key: String, expected: String) {
        val node = wf.nodes.firstOrNull { it.id == nodeId }
        val actual = (node?.config?.get(key) as? WorkflowValue.StringValue)?.value
        assertEquals(expected, actual, "unexpected '$key' on node '$nodeId'")
    }

    private fun assertDoubleConfig(wf: WorkflowDefinition, nodeId: String, key: String, expected: Double) {
        val node = wf.nodes.firstOrNull { it.id == nodeId }
        val actual = (node?.config?.get(key) as? WorkflowValue.DoubleValue)?.value
        assertEquals(expected, actual, "unexpected '$key' on node '$nodeId'")
    }

    @Test fun qwenTxt2ImgIsSound() = assertStructurallySound(qwenTxt2ImgWorkflow)
    @Test fun qwenImg2ImgIsSound() = assertStructurallySound(qwenImg2ImgWorkflow)
    @Test fun wanImg2VidIsSound() = assertStructurallySound(wanImg2VidWorkflow)
    @Test fun wan480pImg2VidIsSound() = assertStructurallySound(wan480pImg2VidWorkflow)
    @Test fun wan14b480pImg2VidIsSound() = assertStructurallySound(wan14b480pImg2VidWorkflow)
    @Test fun qwenTxt2ImgUses2512LightningLora() =
        assertNodePath(qwenTxt2ImgWorkflow, nodeId = "lora0", expected = QWEN_TXT2IMG_LORA)

    @Test fun qwenImg2ImgUses2511EditLightningLora() =
        assertNodePath(qwenImg2ImgWorkflow, nodeId = "lora0", expected = QWEN_IMG2IMG_LORA)

    @Test fun qwenTxt2ImgUsesQ2DiffusionAndQwenFlowShift() {
        assertStringConfig(qwenTxt2ImgWorkflow, nodeId = "diffusion", key = "diffusion_model_path", expected = QWEN_TXT2IMG_DIFFUSION)
        assertDoubleConfig(qwenTxt2ImgWorkflow, nodeId = "sampler", key = "flow_shift", expected = 12.0)
    }

    @Test fun qwenImg2ImgUsesQ2DiffusionAndQwenFlowShift() {
        assertStringConfig(qwenImg2ImgWorkflow, nodeId = "diffusion", key = "diffusion_model_path", expected = QWEN_IMG2IMG_DIFFUSION)
        assertDoubleConfig(qwenImg2ImgWorkflow, nodeId = "sampler", key = "flow_shift", expected = 12.0)
    }
}
