package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import com.ronjunevaldoz.graphyn.plugins.stablesd.SdContextConfig
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdIdCondConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SdServerRequestsTest {

    @Test
    fun photoMakerAndPulidFieldsSerializeOntoTheWireRequest() {
        val request = SdGenerateImageRequest(
            prompt = "a woman img, portrait",
            context = SdContextConfig(
                photoMakerPath = "/models/sdxl/photomaker/photomaker-v2.safetensors",
                pulidWeightsPath = "/models/flux/pulid/pulid_flux_v0.9.1.safetensors",
            ),
            idCond = SdIdCondConfig(
                pmIdEmbedPath = "/tmp/character/id_embeds.bin",
                pmStyleStrength = 15.0,
                pulidIdEmbeddingPath = "/tmp/character/pulid.embed",
                pulidIdWeight = 0.8,
            ),
        )

        val body = Json.parseToJsonElement(imageRequestToJson(request)).jsonObject

        assertEquals(
            "/models/sdxl/photomaker/photomaker-v2.safetensors",
            body.getValue("photoMakerPath").jsonPrimitive.content,
        )
        assertEquals(
            "/models/flux/pulid/pulid_flux_v0.9.1.safetensors",
            body.getValue("pulidWeightsPath").jsonPrimitive.content,
        )
        assertEquals("/tmp/character/id_embeds.bin", body.getValue("pmIdEmbedPath").jsonPrimitive.content)
        assertEquals(15.0f, body.getValue("pmStyleStrength").jsonPrimitive.content.toFloat())
        assertEquals("/tmp/character/pulid.embed", body.getValue("pulidIdEmbeddingPath").jsonPrimitive.content)
        assertEquals(0.8f, body.getValue("pulidIdWeight").jsonPrimitive.content.toFloat())
    }

    @Test
    fun photoMakerAndPulidFieldsDefaultToOffWhenUnset() {
        val request = SdGenerateImageRequest(prompt = "a landscape")

        val body = Json.parseToJsonElement(imageRequestToJson(request)).jsonObject

        assertEquals("", body.getValue("photoMakerPath").jsonPrimitive.content)
        assertEquals("", body.getValue("pulidWeightsPath").jsonPrimitive.content)
        assertEquals("", body.getValue("pmIdEmbedPath").jsonPrimitive.content)
        assertEquals(20.0f, body.getValue("pmStyleStrength").jsonPrimitive.content.toFloat())
        assertEquals("", body.getValue("pulidIdEmbeddingPath").jsonPrimitive.content)
        assertEquals(1.0f, body.getValue("pulidIdWeight").jsonPrimitive.content.toFloat())
    }

    @Test
    fun allInOneModelPathSerializesSeparatelyFromDiffusionModelPath() {
        val request = SdGenerateImageRequest(
            prompt = "a woman img",
            context = SdContextConfig(
                diffusionModelPath = null,
                modelPath = "/models/sdxl/diffusion/sd_xl_base_1.0.safetensors",
            ),
        )

        val body = Json.parseToJsonElement(imageRequestToJson(request)).jsonObject

        assertEquals("", body.getValue("diffusionModelPath").jsonPrimitive.content)
        assertEquals(
            "/models/sdxl/diffusion/sd_xl_base_1.0.safetensors",
            body.getValue("modelPath").jsonPrimitive.content,
        )
    }
}
