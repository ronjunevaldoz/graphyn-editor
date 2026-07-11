package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private const val WAN_DIFFUSION_LOW  = "/models/wan/diffusion/Wan2.2-I2V-A14B-LowNoise-Q4_K_M.gguf"
private const val WAN_DIFFUSION_HIGH = "/models/wan/diffusion/Wan2.2-I2V-A14B-HighNoise-Q4_K_M.gguf"
private const val WAN_T5            = "/models/wan/umt5-xxl-encoder-Q5_K_M.gguf"
private const val WAN_CLIP_VISION   = "/models/wan/clip_vision_h.safetensors"
private const val WAN_VAE           = "/models/wan/wan_2.1_vae.safetensors"
private const val WAN_LORA_DIR      = "/models/wan/lora"
private const val WAN_LORA_LOW      = "$WAN_LORA_DIR/wan2.2_i2v_A14b_low_noise_lora_rank64_lightx2v_4step_1022.safetensors"
private const val WAN_LORA_HIGH     = "$WAN_LORA_DIR/wan2.2_i2v_A14b_high_noise_lora_rank64_lightx2v_4step_1022.safetensors"
private const val WAN_INIT_IMAGE    = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Wan2.2 image-to-video 480P preset using the A14B MoE model family.
 *
 * Wan2.2 A14B is the family that officially supports both 480P and 720P. This preset keeps the
 * 480P size explicit so the launcher can choose a lower-res variant without changing the model
 * family or LoRA wiring.
 */
internal val wan480pImg2VidWorkflow = WorkflowDefinition(
    id = "wan480p-img2vid",
    name = "Wan Image to Video (480p)",
    nodes = listOf(
        guideNote(
            """
            Wan2.2 Image → Video · 480P preset

            Animates a still image using the A14B Wan2.2 MoE model family at
            480P. The same low-noise + high-noise LoRA pair is used here.

            Tip: set init_image on sd.img2vid. Tune video_frames/fps for length.
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf(
                "diffusion_model_path" to WorkflowValue.StringValue(WAN_DIFFUSION_LOW),
                "high_noise_diffusion_model_path" to WorkflowValue.StringValue(WAN_DIFFUSION_HIGH),
            ),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "t5xxl_path" to WorkflowValue.StringValue(WAN_T5),
                "clip_vision_path" to WorkflowValue.StringValue(WAN_CLIP_VISION),
            ),
        ),
        NodeRef(id = "sdvae", type = "sd.vae", config = mapOf("vae_path" to WorkflowValue.StringValue(WAN_VAE))),
        NodeRef(
            id = "sdmodel",
            type = "sd.model",
            config = mapOf("lora_model_dir" to WorkflowValue.StringValue(WAN_LORA_DIR)),
        ),
        NodeRef(
            id = "ctx",
            type = "sd.context",
            config = mapOf(
                "diffusion_flash_attn" to WorkflowValue.BooleanValue(true),
                "n_threads" to WorkflowValue.IntValue(-1),
            ),
        ),
        NodeRef(
            id = "loralow",
            type = "sd.lora",
            config = mapOf(
                "path" to WorkflowValue.StringValue(WAN_LORA_LOW),
                "multiplier" to WorkflowValue.DoubleValue(1.0),
                "is_high_noise" to WorkflowValue.BooleanValue(false),
            ),
        ),
        NodeRef(
            id = "lorahigh",
            type = "sd.lora",
            config = mapOf(
                "path" to WorkflowValue.StringValue(WAN_LORA_HIGH),
                "multiplier" to WorkflowValue.DoubleValue(1.0),
                "is_high_noise" to WorkflowValue.BooleanValue(true),
            ),
        ),
        NodeRef(
            id = "sampler",
            type = "sd.sampler",
            config = mapOf(
                "sample_method" to WorkflowValue.StringValue("euler"),
                "scheduler" to WorkflowValue.StringValue("discrete"),
                "sample_steps" to WorkflowValue.IntValue(4),
                "txt_cfg" to WorkflowValue.DoubleValue(1.0),
                "distilled_guidance" to WorkflowValue.DoubleValue(1.0),
                "flow_shift" to WorkflowValue.DoubleValue(5.0),
            ),
        ),
        NodeRef(
            id = "img2vid",
            type = "sd.img2vid",
            config = mapOf(
                "init_image" to WorkflowValue.StringValue(WAN_INIT_IMAGE),
                "prompt" to WorkflowValue.StringValue("<lora:$WAN_LORA_HIGH:1.0> <lora:$WAN_LORA_LOW:1.0> the scene comes alive, gentle camera push-in, natural motion"),
                "width" to WorkflowValue.IntValue(832),
                "height" to WorkflowValue.IntValue(480),
                "video_frames" to WorkflowValue.IntValue(81),
                "fps" to WorkflowValue.IntValue(16),
                "moe_boundary" to WorkflowValue.DoubleValue(0.875),
                "strength" to WorkflowValue.DoubleValue(1.0),
                "seed" to WorkflowValue.IntValue(-1),
            ),
        ),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("sddiffusion", "diffusion", "sdmodel", "diffusion"),
        ConnectionRef("encoders", "encoders", "sdmodel", "encoders"),
        ConnectionRef("sdvae", "vae", "sdmodel", "vae"),
        ConnectionRef("sdmodel", "model", "ctx", "model"),
        ConnectionRef("ctx", "context", "img2vid", "context"),
        ConnectionRef("loralow", "lora", "img2vid", "loras"),
        ConnectionRef("lorahigh", "lora", "img2vid", "loras"),
        ConnectionRef("sampler", "sampler", "img2vid", "sampler"),
        ConnectionRef("sampler", "sampler", "img2vid", "high_noise_sampler"),
        ConnectionRef("img2vid", "frames", "preview", "value"),
    ),
)
