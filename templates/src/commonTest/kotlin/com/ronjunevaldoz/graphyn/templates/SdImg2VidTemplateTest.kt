package com.ronjunevaldoz.graphyn.templates

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SdImg2VidTemplateTest {

    @Test
    fun buildsMoeChainWithHighNoiseSamplerAndOffload() {
        val def = sdImg2VidWorkflow(
            id = "wan2.2-ti2v-5b",
            name = "Wan 2.2 TI2V 5B",
            paths = SdVideoModelPaths(
                lowNoiseModelPath = "/models/wan/low_noise.gguf",
                highNoiseModelPath = "/models/wan/high_noise.gguf",
                clipVisionPath = "/models/wan/clip_vision.safetensors",
                textEncoderPath = "/models/wan/t5xxl.gguf",
                vaePath = "/models/wan/vae.safetensors",
                maxVram = "10",
            ),
            sampling = SdVideoSamplingDefaults(steps = 20, highNoiseSteps = 10),
        )

        val types = def.nodes.map { it.type }
        assertTrue(types.containsAll(listOf("sd.diffusion", "sd.encoders", "sd.vae", "sd.model", "sd.offload", "sd.context", "sd.vae_tiling", "sd.img2vid")))
        assertEquals(2, def.nodes.count { it.type == "sd.sampler" })
        assertTrue(def.connections.any { it.fromNodeId == "offload" && it.toNodeId == "context" })
        assertTrue(def.connections.any { it.fromNodeId == "high_noise_sampler" && it.toNodeId == "generate" && it.toPort == "high_noise_sampler" })
        assertEquals(StringValue("/models/wan/high_noise.gguf"), def.nodes.single { it.id == "diffusion" }.config["high_noise_diffusion_model_path"])
    }

    @Test
    fun omitsOffloadAndHighNoiseSamplerWhenNotConfigured() {
        val def = sdImg2VidWorkflow(
            id = "wan2.1-i2v-14b",
            name = "Wan 2.1 I2V 14B",
            paths = SdVideoModelPaths(lowNoiseModelPath = "/models/wan/model.gguf"),
            sampling = SdVideoSamplingDefaults(),
        )

        assertTrue(def.nodes.none { it.type == "sd.offload" })
        assertEquals(1, def.nodes.count { it.type == "sd.sampler" })
        assertTrue(def.connections.none { it.toPort == "high_noise_sampler" })
    }
}
