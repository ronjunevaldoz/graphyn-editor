package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// Default model paths — override via config port values at runtime.
private const val FLUX_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val FLUX_CLIP_L    = "/models/flux/text_encoder/clip_l.safetensors"
// t5xxl_Q5_K_M.gguf — t5-v1_1-xxl-encoder-Q3_K_S.gguf (the previous value here) doesn't exist on
// the Modal deployment's model volume or in server-sd's own model catalog (confirmed via
// /api/sd/models/exists), so this path always failed to load. Base Flux schnell's 4-step
// generation runs fine at Q5_K_M without CPU offload (see ImageMotionScene.kt).
private const val FLUX_T5XXL     = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val FLUX_VAE       = "/models/flux/vae/ae.safetensors"

/**
 * FLUX.1-schnell text-to-image workflow — built on the shared `:templates` `sd.*` graph
 * ([sdTxt2ImgWorkflow]); this file only adds the demo-gallery guide note + preview node.
 *
 * FLUX.1-schnell is a 4-step distilled model:
 *   - txt_cfg = 1.0  (CFG is irrelevant for distilled; keep at 1)
 *   - distilled_guidance = 3.5
 *   - flow_shift = 3.0
 *   - negative_prompt = "" (ignored by distilled models)
 */
internal val fluxTxt2ImgWorkflow = sdTxt2ImgWorkflow(
    id = "flux-txt2img",
    name = "FLUX Text to Image",
    paths = SdModelPaths(
        diffusionModelPath = FLUX_DIFFUSION,
        clipLPath = FLUX_CLIP_L,
        t5xxlPath = FLUX_T5XXL,
        vaePath = FLUX_VAE,
    ),
    sampling = SdSamplingDefaults(
        steps = 4,
        cfgScale = 1.0,
        distilledGuidance = 3.5,
        flowShift = 3.0,
        sampleMethod = "euler",
        scheduler = "discrete",
    ),
    prompt = "a cat sitting on a windowsill, golden hour, photorealistic",
    negativePrompt = "",
    width = 1024,
    height = 1024,
).withGalleryPreview(
    outputPort = "image",
    guideText = """
        FLUX.1-schnell Text → Image

        Generates a 1024×1024 image from a text prompt using
        FLUX.1-schnell (4-step distilled, no negative prompt needed).

        Flow: SD Diffusion + SD Encoders + SD VAE → SD Model → SD Context → SD Sampler → SD Text→Image → Preview

        Tip: edit the prompt port on the generate node to change the subject.
        Change width/height for different aspect ratios (multiples of 16).
    """,
)
