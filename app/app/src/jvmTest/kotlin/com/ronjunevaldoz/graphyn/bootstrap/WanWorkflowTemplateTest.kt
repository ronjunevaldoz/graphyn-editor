package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WanWorkflowTemplateTest {
    private companion object {
        const val WAN_DIFFUSION_LOW = "/models/wan/diffusion/Wan2.2-I2V-A14B-LowNoise-Q4_K_M.gguf"
        const val WAN_DIFFUSION_HIGH = "/models/wan/diffusion/Wan2.2-I2V-A14B-HighNoise-Q4_K_M.gguf"
        const val WAN5B_DIFFUSION = "/models/wan/diffusion/Wan2.2-TI2V-5B-Q4_K_M.gguf"
        const val WAN5B_T5 = "/models/wan/umt5-xxl-encoder-Q8_0.gguf"
        const val WAN5B_VAE = "/models/wan/Wan2.2_VAE.safetensors"
        const val WAN14B480P_DIFFUSION = "/models/wan/wan2.1-i2v-14b-480p-Q3_K_S.gguf"
        const val WAN14B480P_LORA =
            "/models/wan/lora/Lightx2v/lightx2v_I2V_14B_480p_cfg_step_distill_rank64_bf16.safetensors"
    }

    @Test
    fun catalogExposesDistinctWanTiers() {
        val names = WorkflowCatalog.entries.map { it.label }.toSet()
        assertTrue("Wan Image to Video (720p)" in names)
        assertTrue("Wan Image to Video (480p)" in names)
        assertTrue("Wan Image to Video (5B)" in names)
        assertTrue("Wan Image to Video (14B 480p, single-model)" in names)
    }

    @Test
    fun wan480pTemplateUsesA14bAnd480pResolution() {
        val workflow = wan480pImg2VidWorkflow

        assertEquals("wan480p-img2vid", workflow.id)
        assertEquals("Wan Image to Video (480p)", workflow.name)
        assertStringConfig(workflow, "sddiffusion", "diffusion_model_path", WAN_DIFFUSION_LOW)
        assertStringConfig(workflow, "sddiffusion", "high_noise_diffusion_model_path", WAN_DIFFUSION_HIGH)
        assertIntConfig(workflow, "img2vid", "width", 832)
        assertIntConfig(workflow, "img2vid", "height", 480)
        assertTrue(workflow.connections.any { it.fromNodeId == "img2vid" && it.toNodeId == "preview" })
    }

    @Test
    fun wan720pTemplateUsesA14bAnd720pResolution() {
        val workflow = wanImg2VidWorkflow

        assertEquals("wan-img2vid", workflow.id)
        assertEquals("Wan Image to Video (720p)", workflow.name)
        assertStringConfig(workflow, "sddiffusion", "diffusion_model_path", WAN_DIFFUSION_LOW)
        assertStringConfig(workflow, "sddiffusion", "high_noise_diffusion_model_path", WAN_DIFFUSION_HIGH)
        assertIntConfig(workflow, "img2vid", "width", 1280)
        assertIntConfig(workflow, "img2vid", "height", 720)
        assertTrue(workflow.connections.any { it.fromNodeId == "img2vid" && it.toNodeId == "preview" })
    }

    @Test
    fun wan5bTemplateUses5bModelAndUpdatedAssets() {
        val workflow = wan5bImg2VidWorkflow

        assertEquals("wan5b-img2vid", workflow.id)
        assertEquals("Wan Image to Video (5B, fits 12GB)", workflow.name)
        assertStringConfig(workflow, "sddiffusion", "diffusion_model_path", WAN5B_DIFFUSION)
        assertStringConfig(workflow, "encoders", "t5xxl_path", WAN5B_T5)
        assertStringConfig(workflow, "sdvae", "vae_path", WAN5B_VAE)
        assertIntConfig(workflow, "img2vid", "width", 480)
        assertIntConfig(workflow, "img2vid", "height", 480)
        assertTrue(workflow.connections.any { it.fromNodeId == "img2vid" && it.toNodeId == "preview" })
    }

    @Test
    fun wan14b480pTemplateIsSingleModelWithOneLora() {
        val workflow = wan14b480pImg2VidWorkflow

        assertEquals("wan14b480p-img2vid", workflow.id)
        assertEquals("Wan Image to Video (14B 480p, single-model)", workflow.name)
        assertStringConfig(workflow, "sddiffusion", "diffusion_model_path", WAN14B480P_DIFFUSION)
        assertTrue("high_noise_diffusion_model_path" !in workflow.nodes.first { it.id == "sddiffusion" }.config)
        assertStringConfig(workflow, "lora", "path", WAN14B480P_LORA)
        assertIntConfig(workflow, "img2vid", "width", 832)
        assertIntConfig(workflow, "img2vid", "height", 480)
        assertTrue(workflow.connections.none { it.toNodeId == "img2vid" && it.toPort == "high_noise_sampler" })
        assertTrue(workflow.connections.any { it.fromNodeId == "img2vid" && it.toNodeId == "preview" })
    }

    private fun assertStringConfig(workflow: WorkflowDefinition, nodeId: String, key: String, expected: String) {
        val value = workflow.nodes.first { it.id == nodeId }.config[key]
        assertEquals(expected, (value as WorkflowValue.StringValue).value)
    }

    private fun assertIntConfig(workflow: WorkflowDefinition, nodeId: String, key: String, expected: Int) {
        val value = workflow.nodes.first { it.id == nodeId }.config[key]
        assertEquals(expected, (value as WorkflowValue.IntValue).value)
    }
}
