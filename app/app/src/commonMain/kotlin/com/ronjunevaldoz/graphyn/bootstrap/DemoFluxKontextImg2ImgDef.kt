package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Reuses the same clip_l/t5xxl/vae as the base Flux schnell templates — Kontext is a distinct
// diffusion checkpoint but the same Flux text/vae encoders, per stable-diffusion.cpp's docs/kontext.md.
private const val FLUX_KONTEXT_DIFFUSION = "/models/flux/diffusion/flux1-kontext-dev-Q4_K_S.gguf"
private const val FLUX_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
private const val FLUX_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val FLUX_VAE = "/models/flux/vae/ae.safetensors"
private const val FLUX_KONTEXT_INIT_IMAGE = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * FLUX.1 Kontext-dev image-to-image — instruction-following edits (e.g. "change 'flux.cpp' to
 * 'kontext.cpp'") on the same Flux diffusion architecture, via a dedicated Kontext checkpoint
 * instead of a LoRA or special mode. Unlike Qwen-Image-Edit, needs no vision-encoder mmproj and
 * runs on 4-6 GB VRAM without CPU offload (see stable-diffusion.cpp's docs/kontext.md) — the base
 * Flux checkpoint has no instruction-following edit capability at all, only this one does.
 *
 * Node graph:
 *   sd.diffusion (Kontext) ─┐
 *   sd.encoders (Flux)      ├─→ sd.model → sd.context → sd.img2img → preview
 *   sd.vae (Flux)          ─┘                  ↑
 *   sd.sampler (cfg=1.0 per docs) ─────────────┘
 *
 * Set init_image on sd.img2img to your source file and write the edit as the prompt.
 */
internal val fluxKontextImg2ImgWorkflow = WorkflowDefinition(
    id = "flux-kontext-img2img",
    name = "FLUX Kontext Image Edit (Image to Image)",
    nodes = listOf(
        guideNote(
            """
            FLUX.1 Kontext-dev Image → Image

            Edits an existing image from an instruction prompt using the
            Kontext checkpoint — a distinct diffusion model from base Flux,
            reusing the same clip_l/t5xxl/vae encoders.

            Tip: set init_image on sd.img2img to your source file and write
            the edit as the prompt (e.g. "change the season to winter").
            cfg-scale 1.0 is recommended by stable-diffusion.cpp's docs.
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf("diffusion_model_path" to WorkflowValue.StringValue(FLUX_KONTEXT_DIFFUSION)),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "clip_l_path" to WorkflowValue.StringValue(FLUX_CLIP_L),
                "t5xxl_path" to WorkflowValue.StringValue(FLUX_T5XXL),
            ),
        ),
        NodeRef(
            id = "sdvae",
            type = "sd.vae",
            config = mapOf("vae_path" to WorkflowValue.StringValue(FLUX_VAE)),
        ),
        NodeRef(id = "sdmodel", type = "sd.model"),
        NodeRef(
            id = "ctx",
            type = "sd.context",
            config = mapOf(
                "diffusion_flash_attn" to WorkflowValue.BooleanValue(true),
                "n_threads" to WorkflowValue.IntValue(-1),
                "clip_on_cpu" to WorkflowValue.BooleanValue(true),
            ),
        ),
        NodeRef(
            id = "sampler",
            type = "sd.sampler",
            config = mapOf(
                "sample_method" to WorkflowValue.StringValue("euler"),
                "scheduler" to WorkflowValue.StringValue("discrete"),
                "sample_steps" to WorkflowValue.IntValue(20),
                "txt_cfg" to WorkflowValue.DoubleValue(1.0),
                "distilled_guidance" to WorkflowValue.DoubleValue(3.5),
                "flow_shift" to WorkflowValue.DoubleValue(3.0),
            ),
        ),
        NodeRef(
            id = "img2img",
            type = "sd.img2img",
            config = mapOf(
                "init_image" to WorkflowValue.StringValue(FLUX_KONTEXT_INIT_IMAGE),
                "prompt" to WorkflowValue.StringValue("change the season to winter, add falling snow"),
                "negative_prompt" to WorkflowValue.StringValue(""),
                "strength" to WorkflowValue.DoubleValue(1.0),
                "seed" to WorkflowValue.IntValue(-1),
                "batch_count" to WorkflowValue.IntValue(1),
            ),
        ),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("sddiffusion", "diffusion", "sdmodel", "diffusion"),
        ConnectionRef("encoders", "encoders", "sdmodel", "encoders"),
        ConnectionRef("sdvae", "vae", "sdmodel", "vae"),
        ConnectionRef("sdmodel", "model", "ctx", "model"),
        ConnectionRef("ctx", "context", "img2img", "context"),
        ConnectionRef("sampler", "sampler", "img2img", "sampler"),
        ConnectionRef("img2img", "image", "preview", "value"),
    ),
)
