package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Default model paths — override via config port values at runtime.
// Q2_K (~6.9 GB) fits a 12 GB card; Q4_K_M (~13 GB) OOMs the diffusion forward pass.
private const val QWEN_DIFFUSION = "/models/qwen/diffusion/qwen-image-2512-Q2_K.gguf"
private const val QWEN_TEXT_ENC  = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct-UD-Q4_K_XL.gguf"
private const val QWEN_VAE       = "/models/qwen/vae/qwen_image_vae.safetensors"
private const val QWEN_LORA_DIR  = "/models/qwen/lora"
// Full server-side LoRA path (the server applies LoRAs by path, not via lora_model_dir).
private const val QWEN_LORA_4STEP = "$QWEN_LORA_DIR/Qwen-Image-2512-Lightning-4steps-V1.0-fp32.safetensors"

/**
 * Qwen-Image (2512) text-to-image with a 4-step Lightning LoRA.
 *
 * Node graph:
 *   sd.diffusion (Qwen-Image diffusion) ─┐
 *   sd.encoders (Qwen2.5-VL llm)         ├─→ sd.model → sd.context → sd.txt2img → preview
 *   sd.vae (Qwen-Image VAE)             ─┘                 ↑ ↑
 *   sd.lora (Lightning 4-step) ─────────────────→ loras ──┘ │
 *   sd.sampler (4-step euler) ─────────────────────────────┘
 *
 * The 4-step Lightning LoRA is wired through the sd.lora node into the txt2img `loras` port
 * (resolved under `lora_model_dir` on sd.model). Qwen-Image is CFG-distilled, so with the
 * LoRA: sample_steps = 4, txt_cfg = 1.0, flow_shift = 3.0.
 */
internal val qwenTxt2ImgWorkflow = WorkflowDefinition(
    id = "qwen-txt2img",
    name = "Qwen Image Text to Image",
    nodes = listOf(
        guideNote(
            """
            Qwen-Image (2512) Text → Image · 4-step LoRA

            Generates an image from a text prompt using Qwen-Image with the
            Lightning 4-step LoRA for fast sampling (no CFG).

            The sd.lora node feeds the txt2img `loras` port; the file
            ($QWEN_LORA_4STEP) is resolved under lora_model_dir on sd.model.

            Tip: keep sample_steps at 4 and txt_cfg at 1.0 while the LoRA is
            attached. Chain more sd.lora nodes into `loras` to stack LoRAs.
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf(
                "diffusion_model_path" to WorkflowValue.StringValue(QWEN_DIFFUSION),
            ),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "llm_path" to WorkflowValue.StringValue(QWEN_TEXT_ENC),
            ),
        ),
        NodeRef(
            id = "sdvae",
            type = "sd.vae",
            config = mapOf(
                "vae_path" to WorkflowValue.StringValue(QWEN_VAE),
            ),
        ),
        NodeRef(
            id = "sdmodel",
            type = "sd.model",
            config = mapOf(
                "lora_model_dir" to WorkflowValue.StringValue(QWEN_LORA_DIR),
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
            id = "lora",
            type = "sd.lora",
            config = mapOf(
                "path"       to WorkflowValue.StringValue(QWEN_LORA_4STEP),
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
                "flow_shift"         to WorkflowValue.DoubleValue(3.0),
            ),
        ),
        NodeRef(
            id = "txt2img",
            type = "sd.txt2img",
            config = mapOf(
                "prompt"          to WorkflowValue.StringValue("a serene mountain lake at sunrise, mist over the water, ultra detailed"),
                "negative_prompt" to WorkflowValue.StringValue(""),
                "width"           to WorkflowValue.IntValue(1328),
                "height"          to WorkflowValue.IntValue(1328),
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
        ConnectionRef("txt2img",     "image",     "preview", "value"),
    ),
)
