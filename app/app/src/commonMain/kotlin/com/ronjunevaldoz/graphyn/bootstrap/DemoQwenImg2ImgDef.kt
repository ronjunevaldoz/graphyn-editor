package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.templates.SdLoraRef
import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// Default model paths — override via config port values at runtime.
// Use the Q2_K edit diffusion by default on 12 GB GPUs so img2img stays resident instead of
// graph-cut paging a Q4_K_M checkpoint through RAM each run.
private const val QWEN_EDIT_DIFFUSION = "/models/qwen/diffusion/qwen-image-edit-2511-Q2_K.gguf"
private const val QWEN_EDIT_TEXT_ENC  = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct-UD-Q4_K_XL.gguf"
// Qwen2.5-VL vision projector — drives the editor's semantic path (sees the reference image).
private const val QWEN_EDIT_MMPROJ    = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct.mmproj-Q8_0.gguf"
private const val QWEN_EDIT_VAE       = "/models/qwen/vae/qwen_image_vae.safetensors"
// Full server-side LoRA path (the server applies LoRAs by path, not via lora_model_dir).
private const val QWEN_EDIT_LORA_4STEP = "/models/qwen/lora/Qwen-Image-Edit-2511-Lightning-4steps-V1.0-fp32.safetensors"
private const val QWEN_EDIT_INIT_IMAGE = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Qwen-Image-Edit (2511) with a 4-step Lightning LoRA — built on the shared `:templates` `sd.*`
 * graph ([sdTxt2ImgWorkflow]); this file only adds the demo-gallery guide note + preview node and
 * the [QWEN_EDIT_INIT_IMAGE] `ref_images` override that activates reference-image conditioning.
 *
 * TODO(qwen-edit-crash): generation currently SIGSEGVs on the server before any output is
 * produced. Confirmed via `docker logs` on the sd host: a native crash inside
 * stable-diffusion.cpp's `ggml_graph_cut` memory planner, while `LLM::LLMRunner::encode_image`
 * (the Qwen2.5-VL vision encoder) processes the `sd.id_cond.ref_images` reference image
 * (`conditioner.hpp:1715 - QwenImageEditPlusPipeline`). Root cause: Qwen-Image-Edit-2511's own
 * components (diffusion + Qwen2.5-VL-7B-Instruct text/vision encoder + mmproj + LoRA) already
 * exceed this 12GB card's VRAM before any reference image is involved, forcing graph-cut to
 * engage just to load — and that specific segment-measurement code path crashes. This is
 * vendored, read-only native code (`native/stable-diffusion.cpp`), so it cannot be patched here.
 * Do not "fix" this workflow by reverting to `sd.img2img`/`init_image` — that only hides the
 * crash by silently skipping real image conditioning (see `docs/architecture/lessons.md`).
 * Remediation paths: (1) file upstream against leejet/stable-diffusion.cpp with the stack trace,
 * or (2) retry with smaller Qwen diffusion/LLM quants so graph-cut isn't needed at all — same
 * fix category as the Kontext t5xxl swap in [DemoFluxKontextImg2ImgDef.kt].
 *
 * Qwen-Image-Edit conditions on the source image as a **reference image** (`--ref-image` / the
 * `sd.id_cond` node — its own port doc explicitly names Qwen alongside PhotoMaker/PuLID), not via
 * `sd.img2img`'s denoising-strength `init_image`. The stable-diffusion.cpp docs' own reference
 * command for this model uses `-r` (ref-image) and never sets `--init-img`/`--strength` at all —
 * using `sd.img2img` here silently ignores the source image's content entirely, regardless of
 * strength, and produces an unrelated image from the prompt alone.
 */
internal val qwenImg2ImgWorkflow = sdTxt2ImgWorkflow(
    id = "qwen-img2img",
    name = "Qwen Image Edit (Image to Image)",
    paths = SdModelPaths(
        diffusionModelPath = QWEN_EDIT_DIFFUSION,
        llmPath = QWEN_EDIT_TEXT_ENC,
        llmVisionPath = QWEN_EDIT_MMPROJ,
        vaePath = QWEN_EDIT_VAE,
        // Mandatory for Qwen-Image-Edit 2511 — without it edit quality degrades badly.
        qwenImageZeroCondT = true,
    ),
    sampling = SdSamplingDefaults(
        steps = 4,
        cfgScale = 1.0,
        distilledGuidance = 1.0,
        flowShift = 12.0,
        sampleMethod = "euler",
        scheduler = "discrete",
        loras = listOf(SdLoraRef(path = QWEN_EDIT_LORA_4STEP, multiplier = 1.0)),
    ),
    prompt = "change the season to winter, add falling snow",
    negativePrompt = "",
).withNodeConfig(
    nodeId = "id_cond",
    overrides = mapOf("ref_images" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue(QWEN_EDIT_INIT_IMAGE)))),
).withGalleryPreview(
    outputPort = "image",
    guideText = """
        Qwen-Image-Edit (2511) · 4-step LoRA

        Edits an existing image from an instruction prompt using
        Qwen-Image-Edit with the Lightning 4-step Edit LoRA.

        The sd.lora node feeds the generate node's `loras` port; the file
        ($QWEN_EDIT_LORA_4STEP) is passed by its full server-side path.
        The source photo is a reference image (sd.id_cond → generate's
        id_cond port), not an sd.img2img init_image.

        Tip: set ref_images on sd.id_cond to your source file and write
        the edit as the prompt (e.g. "make it winter, add snow").
    """,
)
