package com.ronjunevaldoz.graphyn.templates

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.NullValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SdTxt2ImgTemplateTest {

    @Test
    fun buildsFullSdChainWithVaeLorasAndIdCond() {
        val def = sdTxt2ImgWorkflow(
            id = "flux-schnell",
            name = "Flux Schnell",
            paths = SdModelPaths(
                diffusionModelPath = "/models/flux/diffusion.gguf",
                clipLPath = "/models/flux/clip_l.safetensors",
                t5xxlPath = "/models/flux/t5xxl.gguf",
                vaePath = "/models/flux/ae.safetensors",
            ),
            sampling = SdSamplingDefaults(
                steps = 4,
                cfgScale = 1.0,
                distilledGuidance = 3.5,
                flowShift = 3.0,
                loras = listOf(SdLoraRef("/models/flux/lora.safetensors", 0.8)),
            ),
            prompt = "a cat",
            width = 768,
            height = 768,
        )

        val types = def.nodes.map { it.type }
        assertTrue(types.containsAll(listOf("sd.diffusion", "sd.encoders", "sd.vae", "sd.model", "sd.context", "sd.id_cond", "sd.lora", "sd.sampler", "sd.txt2img")))
        assertEquals(StringValue("a cat"), def.nodes.single { it.id == "generate" }.config["prompt"])
        assertEquals(NullValue, def.nodes.single { it.type == "sd.id_cond" }.config["ref_images"])
        assertTrue(def.connections.any { it.fromNodeId == "vae" && it.toNodeId == "model" })
        assertTrue(def.connections.any { it.fromNodeId == "lora0" && it.toNodeId == "generate" && it.toPort == "loras" })
        assertTrue(def.connections.any { it.fromNodeId == "id_cond" && it.toNodeId == "generate" && it.toPort == "id_cond" })
    }

    @Test
    fun omitsVaeNodeAndConnectionWhenVaePathIsNull() {
        val def = sdTxt2ImgWorkflow(
            id = "checkpoint-only",
            name = "Checkpoint",
            paths = SdModelPaths(diffusionModelPath = "/models/sd15/checkpoint.safetensors"),
            sampling = SdSamplingDefaults(),
        )

        assertTrue(def.nodes.none { it.type == "sd.vae" })
        assertTrue(def.connections.none { it.toNodeId == "model" && it.toPort == "vae" })
    }

    @Test
    fun img2imgUsesSameBaseChainWithInitImageAndStrength() {
        val def = sdImg2ImgWorkflow(
            id = "flux-img2img",
            name = "Flux Img2Img",
            paths = SdModelPaths(diffusionModelPath = "/models/flux/diffusion.gguf"),
            sampling = SdSamplingDefaults(),
            initImagePath = "/server/uploads/init.png",
            strength = 0.5,
        )

        val generate = def.nodes.single { it.id == "generate" }
        assertEquals("sd.img2img", generate.type)
        assertEquals(StringValue("/server/uploads/init.png"), generate.config["init_image"])
        assertTrue(def.connections.any { it.fromNodeId == "id_cond" && it.toNodeId == "generate" })
    }
}
