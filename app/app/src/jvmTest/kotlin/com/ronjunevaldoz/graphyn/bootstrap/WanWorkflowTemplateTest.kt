package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WanWorkflowTemplateTest {
    private companion object {
        const val WAN_DIFFUSION_LOW = "/models/diffusion/wan/Wan2.2-I2V-A14B-LowNoise-Q4_K_M.gguf"
        const val WAN_DIFFUSION_HIGH = "/models/diffusion/wan/Wan2.2-I2V-A14B-HighNoise-Q4_K_M.gguf"
        const val WAN5B_DIFFUSION = "/models/wan/Wan2.2-TI2V-5B-Q4_K_M.gguf"
        const val WAN5B_T5 = "/models/wan/umt5_xxl_fp8_e4m3fn_scaled.safetensors"
        const val WAN5B_VAE = "/models/wan/wan2.2_vae.safetensors"
    }

    @Test
    fun catalogExposesDistinctWanTiers() {
        val names = WorkflowCatalog.entries.map { it.label }.toSet()
        assertTrue("Wan Image to Video (720p)" in names)
        assertTrue("Wan Image to Video (480p)" in names)
        assertTrue("Wan Image to Video (5B)" in names)
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

    private fun assertStringConfig(workflow: WorkflowDefinition, nodeId: String, key: String, expected: String) {
        val value = workflow.nodes.first { it.id == nodeId }.config[key]
        assertEquals(expected, (value as WorkflowValue.StringValue).value)
    }

    private fun assertIntConfig(workflow: WorkflowDefinition, nodeId: String, key: String, expected: Int) {
        val value = workflow.nodes.first { it.id == nodeId }.config[key]
        assertEquals(expected, (value as WorkflowValue.IntValue).value)
    }
}
