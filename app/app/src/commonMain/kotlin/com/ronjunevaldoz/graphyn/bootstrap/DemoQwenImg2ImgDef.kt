package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Default model paths — override via config port values at runtime.
// Use the Q2_K edit diffusion by default on 12 GB GPUs so img2img stays resident instead of
// graph-cut paging a Q4_K_M checkpoint through RAM each run.
private const val QWEN_EDIT_DIFFUSION = "/models/qwen/diffusion/qwen-image-edit-2511-Q2_K.gguf"
private const val QWEN_EDIT_TEXT_ENC  = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct-UD-Q4_K_XL.gguf"
// Qwen2.5-VL vision projector — drives the editor's semantic path (sees the reference image).
private const val QWEN_EDIT_MMPROJ    = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct.mmproj-Q8_0.gguf"
private const val QWEN_EDIT_VAE       = "/models/qwen/vae/qwen_image_vae.safetensors"
private const val QWEN_EDIT_LORA_DIR  = "/models/qwen/lora"
// Full server-side LoRA path (the server applies LoRAs by path, not via lora_model_dir).
private const val QWEN_EDIT_LORA_4STEP = "$QWEN_EDIT_LORA_DIR/Qwen-Image-Edit-2511-Lightning-4steps-V1.0-fp32.safetensors"
private const val QWEN_EDIT_INIT_IMAGE = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Qwen-Image-Edit (2511) with a 4-step Lightning LoRA.
 *
 * Node graph:
 *   sd.diffusion (Qwen-Image-Edit) ─┐
 *   sd.encoders (Qwen2.5-VL llm)    ├─→ sd.model → sd.context → sd.txt2img → preview
 *   sd.vae (Qwen-Image VAE)        ─┘                  ↑  ↑  ↑
 *   sd.lora (Edit Lightning 4-step) ───────→ loras ────┘  │  │
 *   sd.sampler (4-step euler) ─────────────────────────────┘  │
 *   sd.id_cond (ref_images = [source photo]) ──────────────────┘
 *
 * Qwen-Image-Edit conditions on the source image as a **reference image** (`--ref-image` / the
 * `sd.id_cond` node — its own port doc explicitly names Qwen alongside PhotoMaker/PuLID), not via
 * `sd.img2img`'s denoising-strength `init_image`. The stable-diffusion.cpp docs' own reference
 * command for this model uses `-r` (ref-image) and never sets `--init-img`/`--strength` at all —
 * using `sd.img2img` here silently ignores the source image's content entirely, regardless of
 * strength, and produces an unrelated image from the prompt alone.
 */
internal val qwenImg2ImgWorkflow = WorkflowDefinition(
    id = "qwen-img2img",
    name = "Qwen Image Edit (Image to Image)",
    nodes = listOf(
        guideNote(
            """
            Qwen-Image-Edit (2511) · 4-step LoRA

            Edits an existing image from an instruction prompt using
            Qwen-Image-Edit with the Lightning 4-step Edit LoRA.

            The sd.lora node feeds the txt2img `loras` port; the file
            ($QWEN_EDIT_LORA_4STEP) is resolved under lora_model_dir on sd.model.
            The source photo is a reference image (sd.id_cond → txt2img's
            id_cond port), not an sd.img2img init_image.

            Tip: set ref_images on sd.id_cond to your source file and write
            the edit as the prompt (e.g. "make it winter, add snow").
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf(
                "diffusion_model_path" to WorkflowValue.StringValue(QWEN_EDIT_DIFFUSION),
            ),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "llm_path"        to WorkflowValue.StringValue(QWEN_EDIT_TEXT_ENC),
                "llm_vision_path" to WorkflowValue.StringValue(QWEN_EDIT_MMPROJ),
            ),
        ),
        NodeRef(
            id = "sdvae",
            type = "sd.vae",
            config = mapOf(
                "vae_path" to WorkflowValue.StringValue(QWEN_EDIT_VAE),
            ),
        ),
        NodeRef(
            id = "sdmodel",
            type = "sd.model",
            config = mapOf(
                "lora_model_dir" to WorkflowValue.StringValue(QWEN_EDIT_LORA_DIR),
            ),
        ),
        NodeRef(
            id = "ctx",
            type = "sd.context",
            config = mapOf(
                "diffusion_flash_attn"    to WorkflowValue.BooleanValue(true),
                "n_threads"               to WorkflowValue.IntValue(-1),
                // Mandatory for Qwen-Image-Edit 2511 — without it edit quality degrades badly.
                "qwen_image_zero_cond_t"  to WorkflowValue.BooleanValue(true),
            ),
        ),
        NodeRef(
            id = "lora",
            type = "sd.lora",
            config = mapOf(
                "path"       to WorkflowValue.StringValue(QWEN_EDIT_LORA_4STEP),
                "multiplier" to WorkflowValue.DoubleValue(1.0),
            ),
        ),
        NodeRef(
            id = "sampler",
            type = "sd.sampler",
            config = mapOf(
                "sample_method"      to WorkflowValue.StringValue("euler"),
                "scheduler"          to WorkflowValue.StringValue("discrete"),
                "sample_steps"       to WorkflowValue.IntValue(4),
                "txt_cfg"            to WorkflowValue.DoubleValue(1.0),
                "distilled_guidance" to WorkflowValue.DoubleValue(1.0),
                "flow_shift"         to WorkflowValue.DoubleValue(12.0),
            ),
        ),
        NodeRef(
            id = "idCond",
            type = "sd.id_cond",
            config = mapOf(
                "ref_images" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue(QWEN_EDIT_INIT_IMAGE))),
            ),
        ),
        NodeRef(
            id = "txt2img",
            type = "sd.txt2img",
            config = mapOf(
                "prompt"          to WorkflowValue.StringValue("change the season to winter, add falling snow"),
                "negative_prompt" to WorkflowValue.StringValue(""),
                "seed"            to WorkflowValue.IntValue(-1),
                "batch_count"     to WorkflowValue.IntValue(1),
            ),
        ),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("sddiffusion", "diffusion", "sdmodel", "diffusion"),
        ConnectionRef("encoders",    "encoders",  "sdmodel", "encoders"),
        ConnectionRef("sdvae",       "vae",       "sdmodel", "vae"),
        ConnectionRef("sdmodel",     "model",     "ctx",     "model"),
        ConnectionRef("ctx",         "context",   "txt2img", "context"),
        ConnectionRef("lora",        "lora",      "txt2img", "loras"),
        ConnectionRef("sampler",     "sampler",   "txt2img", "sampler"),
        ConnectionRef("idCond",      "id_cond",   "txt2img", "id_cond"),
        ConnectionRef("txt2img",     "image",     "preview", "value"),
    ),
)
