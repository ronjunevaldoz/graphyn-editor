package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

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
 * Node graph:
 *   sd.diffusion (Kontext) ─┐
 *   sd.encoders (Flux)      ├─→ sd.model → sd.context → sd.txt2img → preview
 *   sd.vae (Flux)          ─┘                  ↑  ↑
 *   sd.sampler (cfg=1.0 per docs) ──────────────┘  │
 *   sd.id_cond (ref_images = [source photo]) ───────┘
 *
 * Kontext conditions on the source image as a **reference image** (`-r` / `--ref-image`), same as
 * Qwen-Image-Edit — the docs' own reference command never sets `--init-img`/`--strength` at all.
 * Using `sd.img2img`'s denoising-strength `init_image` here silently ignores the source image's
 * content entirely and produces an unrelated image from the prompt alone (confirmed the hard way
 * on the Qwen-Image-Edit template first — same fix applies here).
 */
internal val fluxKontextImg2ImgWorkflow = WorkflowDefinition(
    id = "flux-kontext-img2img",
    name = "FLUX Kontext Image Edit (Image to Image)",
    nodes = listOf(
        guideNote(
            """
            FLUX.1 Kontext-dev

            Edits an existing image from an instruction prompt using the
            Kontext checkpoint — a distinct diffusion model from base Flux,
            reusing the same clip_l/t5xxl/vae encoders. The source photo is
            a reference image (sd.id_cond → txt2img's id_cond port), not an
            sd.img2img init_image.

            Tip: set ref_images on sd.id_cond to your source file and write
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
            id = "idCond",
            type = "sd.id_cond",
            config = mapOf(
                "ref_images" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue(FLUX_KONTEXT_INIT_IMAGE))),
            ),
        ),
        NodeRef(
            id = "txt2img",
            type = "sd.txt2img",
            config = mapOf(
                "prompt" to WorkflowValue.StringValue("change the season to winter, add falling snow"),
                "negative_prompt" to WorkflowValue.StringValue(""),
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
        ConnectionRef("ctx", "context", "txt2img", "context"),
        ConnectionRef("sampler", "sampler", "txt2img", "sampler"),
        ConnectionRef("idCond", "id_cond", "txt2img", "id_cond"),
        ConnectionRef("txt2img", "image", "preview", "value"),
    ),
)
