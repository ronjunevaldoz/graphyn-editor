package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Wan2.1-I2V-14B-480P Q3_K_S (7.93 GB): single dense model (no MoE high/low-noise pair), trained
// natively at 480x832 so no positional-embedding extrapolation happens at this resolution — best
// VRAM fit for a 14B model on a 12 GB card. Reuses the A14B family's umt5 + clip_vision + wan_2.1
// VAE (server-sd's ModelResolver resolves the same encoder/vae paths for both).
private const val WAN14B480P_DIFFUSION   = "/models/wan/wan2.1-i2v-14b-480p-Q3_K_S.gguf"
private const val WAN14B480P_T5          = "/models/wan/umt5-xxl-encoder-Q5_K_M.gguf"
private const val WAN14B480P_CLIP_VISION = "/models/wan/clip_vision_h.safetensors"
private const val WAN14B480P_VAE         = "/models/wan/wan_2.1_vae.safetensors"
private const val WAN14B480P_LORA_DIR    = "/models/wan/lora/Lightx2v"
private const val WAN14B480P_LORA        = "$WAN14B480P_LORA_DIR/lightx2v_I2V_14B_480p_cfg_step_distill_rank64_bf16.safetensors"
private const val WAN14B480P_INIT_IMAGE  = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Wan2.1-I2V-14B-480P image-to-video — single-model 14B tier, distilled to 4 steps.
 *
 * Distinct from the A14B workflows ([wanImg2VidWorkflow], [wan480pImg2VidWorkflow]): this is a
 * dense single model (no high-noise expert, no `high_noise_sampler`, one LoRA not a MoE pair) —
 * same shape as [wan5bImg2VidWorkflow] but a bigger 14B model at its native 480x832 resolution.
 * cfg_scale=1.0 + flow_shift=3.0 + LoRA strength=0.7 per the lightx2v distillation guide.
 */
internal val wan14b480pImg2VidWorkflow = WorkflowDefinition(
    id = "wan14b480p-img2vid",
    name = "Wan Image to Video (14B 480p, single-model)",
    nodes = listOf(
        guideNote(
            """
            Wan2.1-I2V-14B-480P Image → Video · single-model, 4-step distilled

            A dense 14B model trained natively at 480x832, paired with a 4-step
            lightx2v LoRA — no MoE high-noise pass, unlike the A14B tiers.

            Tip: set init_image on sd.img2vid. cfg_scale=1.0, flow_shift=3.0,
            LoRA strength=0.7 are tuned for this distillation LoRA.
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf(
                "diffusion_model_path" to WorkflowValue.StringValue(WAN14B480P_DIFFUSION),
            ),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "t5xxl_path"       to WorkflowValue.StringValue(WAN14B480P_T5),
                "clip_vision_path" to WorkflowValue.StringValue(WAN14B480P_CLIP_VISION),
            ),
        ),
        NodeRef(
            id = "sdvae",
            type = "sd.vae",
            config = mapOf("vae_path" to WorkflowValue.StringValue(WAN14B480P_VAE)),
        ),
        NodeRef(
            id = "sdmodel",
            type = "sd.model",
            config = mapOf("lora_model_dir" to WorkflowValue.StringValue(WAN14B480P_LORA_DIR)),
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
                "path"          to WorkflowValue.StringValue(WAN14B480P_LORA),
                "multiplier"    to WorkflowValue.DoubleValue(0.7),
                "is_high_noise" to WorkflowValue.BooleanValue(false),
            ),
        ),
        NodeRef(
            id = "sampler",
            type = "sd.sampler",
            config = mapOf(
                "sample_method" to WorkflowValue.StringValue("euler"),
                "scheduler"     to WorkflowValue.StringValue("discrete"),
                "sample_steps"  to WorkflowValue.IntValue(4),
                "txt_cfg"       to WorkflowValue.DoubleValue(1.0),
                "flow_shift"    to WorkflowValue.DoubleValue(3.0),
            ),
        ),
        NodeRef(
            id = "img2vid",
            type = "sd.img2vid",
            config = mapOf(
                "init_image"   to WorkflowValue.StringValue(WAN14B480P_INIT_IMAGE),
                "prompt"       to WorkflowValue.StringValue("the scene comes alive, gentle camera push-in, natural motion"),
                "width"        to WorkflowValue.IntValue(832),
                "height"       to WorkflowValue.IntValue(480),
                "video_frames" to WorkflowValue.IntValue(48),
                "fps"          to WorkflowValue.IntValue(16),
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
        ConnectionRef("lora",        "lora",      "img2vid", "loras"),
        ConnectionRef("sampler",     "sampler",   "img2vid", "sampler"),
        ConnectionRef("img2vid",     "frames",    "preview", "value"),
    ),
)
