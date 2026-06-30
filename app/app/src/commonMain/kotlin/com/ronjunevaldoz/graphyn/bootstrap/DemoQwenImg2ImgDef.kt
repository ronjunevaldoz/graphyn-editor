package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Default model paths — override via config port values at runtime.
private const val QWEN_EDIT_DIFFUSION = "/models/qwen-image-edit/Qwen-Image-Edit-2511-Q4_K.gguf"
private const val QWEN_EDIT_TEXT_ENC  = "/models/qwen-image-edit/text_encoder/qwen_2.5_vl_7b-Q4_K.gguf"
private const val QWEN_EDIT_VAE       = "/models/qwen-image-edit/vae/qwen_image_vae.safetensors"
private const val QWEN_EDIT_LORA_DIR  = "/models/qwen-image-edit/lora"
// 4-step Edit LoRA applied via inline <lora:name:weight> prompt syntax, resolved under
// lora_model_dir by stable-diffusion.cpp at runtime.
private const val QWEN_EDIT_LORA_4STEP = "Qwen-Image-Edit-Lightning-4steps-V1.0"
private const val QWEN_EDIT_INIT_IMAGE = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Qwen-Image-Edit (2511) image-to-image with a 4-step Lightning LoRA.
 *
 * Node graph:
 *   sd.diffusion (Qwen-Image-Edit) ─┐
 *   sd.encoders (Qwen2.5-VL llm)    ├─→ sd.model → sd.context → sd.img2img → preview
 *   sd.vae (Qwen-Image VAE)        ─┘                  ↑
 *   sd.sampler (4-step euler) ─────────────────────────┘
 *
 * Qwen-Image-Edit takes the source image (init_image) plus an edit-instruction prompt. The
 * 4-step Edit LoRA is applied inline via `<lora:…:1.0>` (resolved under `lora_model_dir` on
 * sd.model): sample_steps = 4, txt_cfg = 1.0, strength = 1.0 (the edit model preserves structure).
 */
internal val qwenImg2ImgWorkflow = WorkflowDefinition(
    id = "qwen-img2img",
    name = "Qwen Image Edit (Image to Image)",
    nodes = listOf(
        guideNote(
            """
            Qwen-Image-Edit (2511) Image → Image · 4-step LoRA

            Edits an existing image from an instruction prompt using
            Qwen-Image-Edit with the Lightning 4-step Edit LoRA.

            The 4-step LoRA is applied inline in the prompt:
                <lora:$QWEN_EDIT_LORA_4STEP:1.0>
            resolved under lora_model_dir on sd.model.

            Tip: set init_image on sd.img2img to your source file and write
            the edit after the <lora:…> tag (e.g. "make it winter, add snow").
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
                "llm_path" to WorkflowValue.StringValue(QWEN_EDIT_TEXT_ENC),
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
                "diffusion_flash_attn" to WorkflowValue.BooleanValue(true),
                "n_threads"            to WorkflowValue.IntValue(-1),
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
                "flow_shift"         to WorkflowValue.DoubleValue(3.0),
            ),
        ),
        NodeRef(
            id = "img2img",
            type = "sd.img2img",
            config = mapOf(
                "init_image"      to WorkflowValue.StringValue(QWEN_EDIT_INIT_IMAGE),
                "prompt"          to WorkflowValue.StringValue("<lora:$QWEN_EDIT_LORA_4STEP:1.0> change the season to winter, add falling snow"),
                "negative_prompt" to WorkflowValue.StringValue(""),
                "strength"        to WorkflowValue.DoubleValue(1.0),
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
        ConnectionRef("ctx",         "context",   "img2img", "context"),
        ConnectionRef("sampler",     "sampler",   "img2img", "sampler"),
        ConnectionRef("img2img",     "image",     "preview", "value"),
    ),
)
