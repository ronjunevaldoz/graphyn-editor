package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// SDXL 1.0 base + VAE — any SDXL checkpoint works per stable-diffusion.cpp's docs/photo_maker.md.
private const val SDXL_DIFFUSION = "/models/sdxl/diffusion/sd_xl_base_1.0.safetensors"
private const val SDXL_VAE = "/models/sdxl/vae/sdxl_vae.safetensors"
private const val PHOTOMAKER_V2_WEIGHTS = "/models/sdxl/photomaker/photomaker-v2.safetensors"
// Placeholder — a real character's precomputed id_embeds.bin path. This demo ships inert (blank
// path -> id_cond effectively becomes a no-op reference) until a user runs face_detect.py locally
// and points this at a real file, or uses the "character reference -> generate samples" workflow.
private const val PLACEHOLDER_ID_EMBED_PATH = ""

/**
 * SDXL 1.0 + PhotoMaker v2 — identity-preserving generation from a precomputed id_embeds.bin.
 * Built on the shared `:templates` `sd.*` graph ([sdTxt2ImgWorkflow]); this file adds the
 * demo-gallery guide note + preview node, the `photo_maker_path` override on the `model` node
 * (an `sdContextPathPorts` field — see `SdContextPathPorts.kt` — not modeled by [SdModelPaths]),
 * and the `pm_id_embed_path` override on `id_cond`.
 *
 * SDXL ships as an all-in-one checkpoint (UNet + both CLIP encoders + VAE in one file) and must
 * load via `sd.diffusion`'s `model_path` port, not `diffusion_model_path` — the latter prefix-remaps
 * for split-format FLUX/SD3 releases and would skip loading the encoders baked into an SDXL
 * checkpoint (confirmed against native/stable-diffusion.cpp/src/stable-diffusion.cpp's model-load
 * code: `model_path` loads with no prefix, `diffusion_model_path` loads with a
 * "model.diffusion_model." prefix remap). [SdModelPaths] has no `modelPath` field (shared by
 * working FLUX/Qwen demos that are correctly split-format), so this clears the base template's
 * `diffusion_model_path` and sets `model_path` instead via the same post-hoc override mechanism
 * used for `photo_maker_path`.
 *
 * Unlike FLUX Kontext / Qwen-Image-Edit (pure ref-image conditioning), PhotoMaker also requires a
 * class-word + trigger-word convention in the prompt itself (e.g. "a woman img, ...") per
 * stable-diffusion.cpp's docs/photo_maker.md — the trigger word "img" is hard-coded upstream.
 *
 * Requires a one-time local precompute step before this workflow's `pm_id_embed_path` points at a
 * real file: `python native/stable-diffusion.cpp/script/face_detect.py <folder_of_photos>` writes
 * `id_embeds.bin` into that folder. See [characterReferenceSamplesWorkflow] for a CLI-driven
 * variant that documents/invokes this step.
 */
internal val photoMakerSdxlWorkflow = sdTxt2ImgWorkflow(
    id = "photomaker-sdxl",
    name = "SDXL + PhotoMaker v2 (Identity-Preserving)",
    paths = SdModelPaths(
        diffusionModelPath = "", // cleared; model_path set below via .withNodeConfig
        vaePath = SDXL_VAE,
    ),
    sampling = SdSamplingDefaults(
        steps = 30,
        cfgScale = 5.0,
        sampleMethod = "euler",
        scheduler = "discrete",
    ),
    prompt = "a woman img, retro futurism poster art, intricate details, masterpiece, best quality",
    negativePrompt = "realistic, photo-realistic, worst quality, greyscale, bad anatomy, bad hands, error, text",
    width = 1024,
    height = 1024,
).withNodeConfig(
    nodeId = "diffusion",
    overrides = mapOf("model_path" to WorkflowValue.StringValue(SDXL_DIFFUSION)),
).withNodeConfig(
    nodeId = "model",
    overrides = mapOf("photo_maker_path" to WorkflowValue.StringValue(PHOTOMAKER_V2_WEIGHTS)),
).withNodeConfig(
    nodeId = "id_cond",
    overrides = mapOf(
        "pm_id_embed_path" to WorkflowValue.StringValue(PLACEHOLDER_ID_EMBED_PATH),
        "pm_style_strength" to WorkflowValue.DoubleValue(20.0),
    ),
).withGalleryPreview(
    outputPort = "image",
    guideText = """
        SDXL 1.0 + PhotoMaker v2

        Identity-preserving generation from a precomputed id_embeds.bin
        (run face_detect.py once per character — see the "character
        reference -> generate samples" workflow for a guided version).

        Prompt must include a class word (man/woman/girl/boy) followed
        by the trigger word "img" (e.g. "a woman img, ..."). cfg-scale
        5.0 and 1024x1024 are recommended by stable-diffusion.cpp docs.

        Tip: set pm_id_embed_path on sd.id_cond to your character's
        id_embeds.bin path before running.
    """,
)
