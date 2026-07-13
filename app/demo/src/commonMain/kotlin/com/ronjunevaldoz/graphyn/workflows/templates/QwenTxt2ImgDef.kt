package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.templates.SdLoraRef
import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// Default model paths — override via config port values at runtime.
// Use the Q2_K diffusion by default on 12 GB GPUs: it stays resident instead of paging a Q4_K_M
// checkpoint through graph-cut offload every run.
private const val QWEN_DIFFUSION = "/models/qwen/diffusion/qwen-image-2512-Q2_K.gguf"
private const val QWEN_TEXT_ENC  = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct-UD-Q4_K_XL.gguf"
private const val QWEN_VAE       = "/models/qwen/vae/qwen_image_vae.safetensors"
// Full server-side LoRA path (the server applies LoRAs by path, not via lora_model_dir).
private const val QWEN_LORA_4STEP = "/models/qwen/lora/Qwen-Image-2512-Lightning-4steps-V1.0-fp32.safetensors"

/**
 * Qwen-Image (2512) text-to-image with a 4-step Lightning LoRA — built on the shared `:templates`
 * `sd.*` graph ([sdTxt2ImgWorkflow]); this file only adds the demo-gallery guide note + preview
 * node. Qwen-Image is CFG-distilled, so with the LoRA: sample_steps = 4, txt_cfg = 1.0,
 * flow_shift = 12.0.
 */
internal val qwenTxt2ImgWorkflow = sdTxt2ImgWorkflow(
    id = "qwen-txt2img",
    name = "Qwen Image Text to Image",
    paths = SdModelPaths(
        diffusionModelPath = QWEN_DIFFUSION,
        llmPath = QWEN_TEXT_ENC,
        vaePath = QWEN_VAE,
    ),
    sampling = SdSamplingDefaults(
        steps = 4,
        cfgScale = 1.0,
        distilledGuidance = 1.0,
        flowShift = 12.0,
        sampleMethod = "euler",
        scheduler = "discrete",
        loras = listOf(SdLoraRef(path = QWEN_LORA_4STEP, multiplier = 1.0)),
    ),
    prompt = "a serene mountain lake at sunrise, mist over the water, ultra detailed",
    negativePrompt = "",
    width = 1024,
    height = 1024,
).withGalleryPreview(
    outputPort = "image",
    guideText = """
        Qwen-Image (2512) Text → Image · 4-step LoRA

        Generates an image from a text prompt using Qwen-Image with the
        Lightning 4-step LoRA for fast sampling (no CFG).

        The sd.lora node feeds the generate node's `loras` port; the file
        ($QWEN_LORA_4STEP) is passed by its full server-side path.

        Tip: keep sample_steps at 4 and txt_cfg at 1.0 while the LoRA is
        attached. Chain more sd.lora nodes into `loras` to stack LoRAs.
    """,
)
