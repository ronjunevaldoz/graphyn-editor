package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Default model paths — override via config port values at runtime.
private const val WAN_DIFFUSION_LOW  = "/models/wan/wan2.2_i2v_low_noise-Q4_K.gguf"
private const val WAN_DIFFUSION_HIGH = "/models/wan/wan2.2_i2v_high_noise-Q4_K.gguf"
private const val WAN_T5            = "/models/wan/text_encoder/umt5_xxl-Q4_K.gguf"
private const val WAN_CLIP_VISION   = "/models/wan/clip_vision/clip_vision_h.safetensors"
private const val WAN_VAE           = "/models/wan/vae/wan_2.1_vae.safetensors"
private const val WAN_LORA_DIR      = "/models/wan/lora"
// Full server-side LoRA paths. Wan2.2 is MoE — the high-noise sd.lora carries is_high_noise=true.
private const val WAN_LORA_LOW      = "$WAN_LORA_DIR/lightx2v_I2V_14B_low_noise_4step.safetensors"
private const val WAN_LORA_HIGH     = "$WAN_LORA_DIR/lightx2v_I2V_14B_high_noise_4step.safetensors"
private const val WAN_INIT_IMAGE    = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Wan2.2 image-to-video with 4-step LightX2V LoRAs.
 *
 * Wan2.2 is a Mixture-of-Experts (MoE) model: a high-noise diffusion model runs the early
 * denoising steps and a low-noise model finishes. Each expert has its own 4-step LoRA — two
 * sd.lora nodes fan into the single img2vid `loras` port (the high-noise one sets
 * is_high_noise=true). LoRA files resolve under `lora_model_dir` on sd.model.
 *
 * Node graph:
 *   sd.diffusion (low + high noise) ─┐
 *   sd.encoders (umt5 + clip_vision) ├─→ sd.model → sd.context → sd.img2vid → preview
 *   sd.vae (Wan VAE)                ─┘                  ↑ ↑
 *   sd.lora low + sd.lora high ───────────→ loras ─────┘ │
 *   sd.sampler (4-step) ───────────────→ sampler + high_noise_sampler
 */
internal val wanImg2VidWorkflow = WorkflowDefinition(
    id = "wan-img2vid",
    name = "Wan Image to Video",
    nodes = listOf(
        guideNote(
            """
            Wan2.2 Image → Video · 4-step LightX2V LoRAs

            Animates a still image into a short clip using Wan2.2 (MoE) with
            the 4-step LightX2V acceleration LoRAs. Two sd.lora nodes fan into
            the img2vid `loras` port — high-noise ($WAN_LORA_HIGH) and
            low-noise ($WAN_LORA_LOW).

            Tip: set init_image on sd.img2vid. Tune video_frames/fps for length.
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf(
                "diffusion_model_path"            to WorkflowValue.StringValue(WAN_DIFFUSION_LOW),
                "high_noise_diffusion_model_path" to WorkflowValue.StringValue(WAN_DIFFUSION_HIGH),
            ),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "t5xxl_path"       to WorkflowValue.StringValue(WAN_T5),
                "clip_vision_path" to WorkflowValue.StringValue(WAN_CLIP_VISION),
            ),
        ),
        NodeRef(
            id = "sdvae",
            type = "sd.vae",
            config = mapOf(
                "vae_path" to WorkflowValue.StringValue(WAN_VAE),
            ),
        ),
        NodeRef(
            id = "sdmodel",
            type = "sd.model",
            config = mapOf(
                "lora_model_dir" to WorkflowValue.StringValue(WAN_LORA_DIR),
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
            id = "loralow",
            type = "sd.lora",
            config = mapOf(
                "path"          to WorkflowValue.StringValue(WAN_LORA_LOW),
                "multiplier"    to WorkflowValue.DoubleValue(1.0),
                "is_high_noise" to WorkflowValue.BooleanValue(false),
            ),
        ),
        NodeRef(
            id = "lorahigh",
            type = "sd.lora",
            config = mapOf(
                "path"          to WorkflowValue.StringValue(WAN_LORA_HIGH),
                "multiplier"    to WorkflowValue.DoubleValue(1.0),
                "is_high_noise" to WorkflowValue.BooleanValue(true),
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
                "flow_shift"         to WorkflowValue.DoubleValue(5.0),
            ),
        ),
        NodeRef(
            id = "img2vid",
            type = "sd.img2vid",
            config = mapOf(
                "init_image"   to WorkflowValue.StringValue(WAN_INIT_IMAGE),
                "prompt"       to WorkflowValue.StringValue("<lora:$WAN_LORA_HIGH:1.0> <lora:$WAN_LORA_LOW:1.0> the scene comes alive, gentle camera push-in, natural motion"),
                "width"        to WorkflowValue.IntValue(832),
                "height"       to WorkflowValue.IntValue(480),
                "video_frames" to WorkflowValue.IntValue(81),
                "fps"          to WorkflowValue.IntValue(16),
                "moe_boundary" to WorkflowValue.DoubleValue(0.875),
                "strength"     to WorkflowValue.DoubleValue(1.0),
                "seed"         to WorkflowValue.IntValue(-1),
            ),
        ),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("sddiffusion", "diffusion", "sdmodel", "diffusion"),
        ConnectionRef("encoders",    "encoders",  "sdmodel", "encoders"),
        ConnectionRef("sdvae",       "vae",       "sdmodel", "vae"),
        ConnectionRef("sdmodel",     "model",     "ctx",     "model"),
        ConnectionRef("ctx",         "context",   "img2vid", "context"),
        ConnectionRef("loralow",     "lora",      "img2vid", "loras"),
        ConnectionRef("lorahigh",    "lora",      "img2vid", "loras"),
        ConnectionRef("sampler",     "sampler",   "img2vid", "sampler"),
        ConnectionRef("sampler",     "sampler",   "img2vid", "high_noise_sampler"),
        ConnectionRef("img2vid",     "frames",    "preview", "value"),
    ),
)
