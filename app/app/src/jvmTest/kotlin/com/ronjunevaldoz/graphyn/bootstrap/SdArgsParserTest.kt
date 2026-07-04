package com.ronjunevaldoz.graphyn.bootstrap

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SdArgsParserTest {

    @Test fun argsToJsonEncodesValuesAndBooleanFlagsCorrectly() {
        val args = listOf(
            "--prompt", "a fox in a forest",
            "--negative-prompt", "blurry",
            "--width", "1024", "--height", "1024",
            "--cfg-scale", "3.5", "--seed", "42",
            "--diffusion-model", "/models/flux/diffusion.gguf",
            "--vae-tiling", // boolean flag — must NOT consume "--hires" as its value
            "--hires", // boolean flag — must NOT consume "--qwen-image-zero-cond-t" as its value
            "--qwen-image-zero-cond-t", // boolean flag
        )
        val json = Json.parseToJsonElement(argsToJson(args)).jsonObject

        assertEquals("a fox in a forest", json["prompt"]?.jsonPrimitive?.content)
        assertEquals("blurry", json["negativePrompt"]?.jsonPrimitive?.content)
        assertEquals(1024, json["width"]?.jsonPrimitive?.int)
        assertEquals(1024, json["height"]?.jsonPrimitive?.int)
        assertEquals(3.5f, json["cfgScale"]?.jsonPrimitive?.float)
        assertEquals(42L, json["seed"]?.jsonPrimitive?.long)
        assertEquals("/models/flux/diffusion.gguf", json["diffusionModelPath"]?.jsonPrimitive?.content)
        assertTrue(json["vaeTilingEnabled"]?.jsonPrimitive?.boolean == true)
        assertTrue(json["hiresEnabled"]?.jsonPrimitive?.boolean == true)
        assertTrue(json["qwenImageZeroCondT"]?.jsonPrimitive?.boolean == true)
    }

    @Test fun unknownValueFlagIsNotMistakenForABooleanFlag() {
        // A value-taking flag not yet in BOOLEAN_FLAGS must still consume its value correctly —
        // confirms the "assume value-consuming unless known-boolean" default works as intended.
        val args = listOf("--prompt", "p", "--some-new-flag", "some-value", "--width", "512")
        val json = Json.parseToJsonElement(argsToJson(args)).jsonObject
        assertEquals(512, json["width"]?.jsonPrimitive?.int)
    }

    @Test fun videoArgsToJsonLiftsLoraTagsAndInfersHighNoise() {
        val args = listOf(
            "--prompt", "<lora:wan_high_noise_v1:0.8>",
            "--prompt", "a dragon flying",
            "--width", "832", "--height", "480",
            "--video-frames", "81", "--fps", "16",
        )
        val json = Json.parseToJsonElement(videoArgsToJson(args)).jsonObject

        assertEquals("a dragon flying", json["prompt"]?.jsonPrimitive?.content)
        val loraPaths = json["loraPaths"]?.toString().orEmpty()
        val loraHighNoise = json["loraHighNoise"]?.toString().orEmpty()
        assertTrue(loraPaths.contains("wan_high_noise_v1"), "expected lora path in $loraPaths")
        assertTrue(loraHighNoise.contains("true"), "expected high-noise flag in $loraHighNoise")
        assertFalse(loraHighNoise.contains("false"), "expected only true entries in $loraHighNoise")
    }
}
