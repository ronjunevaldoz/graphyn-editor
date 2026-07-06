package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// Reuses the same clip_l/t5xxl/vae as the base Flux schnell templates — Kontext is a distinct
// diffusion checkpoint but the same Flux text/vae encoders, per stable-diffusion.cpp's docs/kontext.md.
private const val FLUX_KONTEXT_DIFFUSION = "/models/flux/diffusion/flux1-kontext-dev-Q4_K_S.gguf"
private const val FLUX_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
// Q3_K_S instead of Q5_K_M — the larger quant left almost no VRAM headroom for inference on a
// 12GB card once combined with Kontext's own diffusion weights (confirmed: Kontext + clip_l +
// t5xxl_Q5_K_M + vae alone used ~11GB, forcing CPU offload and 30+ minute generations).
private const val FLUX_T5XXL = "/models/flux/text_encoder/t5-v1_1-xxl-encoder-Q3_K_S.gguf"
private const val FLUX_VAE = "/models/flux/vae/ae.safetensors"
private const val FLUX_KONTEXT_INIT_IMAGE = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * FLUX.1 Kontext-dev — instruction-following edits (e.g. "change 'flux.cpp' to 'kontext.cpp'") on
 * the same Flux diffusion architecture, via a dedicated Kontext checkpoint instead of a LoRA or
 * special mode. Unlike Qwen-Image-Edit, needs no vision-encoder mmproj and runs on 4-6 GB VRAM
 * without CPU offload (see stable-diffusion.cpp's docs/kontext.md) — the base Flux checkpoint has
 * no instruction-following edit capability at all, only this one does.
 *
 * Built on the shared `:templates` `sd.*` graph ([sdTxt2ImgWorkflow]); this file only adds the
 * demo-gallery guide note + preview node, the `clip_on_cpu` context override (not modeled by
 * [SdModelPaths] — Kontext-specific), and the [FLUX_KONTEXT_INIT_IMAGE] `ref_images` override.
 *
 * Kontext conditions on the source image as a **reference image** (`-r` / `--ref-image`), same as
 * Qwen-Image-Edit — the docs' own reference command never sets `--init-img`/`--strength` at all.
 * Using `sd.img2img`'s denoising-strength `init_image` here silently ignores the source image's
 * content entirely and produces an unrelated image from the prompt alone (confirmed the hard way
 * on the Qwen-Image-Edit template first — same fix applies here).
 */
internal val fluxKontextImg2ImgWorkflow = sdTxt2ImgWorkflow(
    id = "flux-kontext-img2img",
    name = "FLUX Kontext Image Edit (Image to Image)",
    paths = SdModelPaths(
        diffusionModelPath = FLUX_KONTEXT_DIFFUSION,
        clipLPath = FLUX_CLIP_L,
        t5xxlPath = FLUX_T5XXL,
        vaePath = FLUX_VAE,
    ),
    sampling = SdSamplingDefaults(
        steps = 20,
        cfgScale = 1.0,
        distilledGuidance = 3.5,
        flowShift = 3.0,
        sampleMethod = "euler",
        scheduler = "discrete",
    ),
    prompt = "change the season to winter, add falling snow",
    negativePrompt = "",
).withNodeConfig(
    nodeId = "context",
    overrides = mapOf("clip_on_cpu" to WorkflowValue.BooleanValue(true)),
).withNodeConfig(
    nodeId = "id_cond",
    overrides = mapOf("ref_images" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue(FLUX_KONTEXT_INIT_IMAGE)))),
).withGalleryPreview(
    outputPort = "image",
    guideText = """
        FLUX.1 Kontext-dev

        Edits an existing image from an instruction prompt using the
        Kontext checkpoint — a distinct diffusion model from base Flux,
        reusing the same clip_l/t5xxl/vae encoders. The source photo is
        a reference image (sd.id_cond → generate's id_cond port), not an
        sd.img2img init_image.

        Tip: set ref_images on sd.id_cond to your source file and write
        the edit as the prompt (e.g. "change the season to winter").
        cfg-scale 1.0 is recommended by stable-diffusion.cpp's docs.
    """,
)
