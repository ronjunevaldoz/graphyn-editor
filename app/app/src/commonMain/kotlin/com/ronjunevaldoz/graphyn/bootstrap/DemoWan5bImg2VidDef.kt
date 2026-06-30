package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Wan2.2-TI2V-5B: a dense single-stream model (no high/low-noise MoE pair), so it fits a 12 GB card
// where the A14B (~25.8 GB) cannot. Uses the Wan2.2 VAE (not 2.1) and the umt5 text encoder.
private const val WAN5B_DIFFUSION   = "/models/wan/Wan2.2-TI2V-5B-Q4_K_M.gguf"
private const val WAN5B_T5          = "/models/wan/umt5-xxl-encoder-Q5_K_M.gguf"
private const val WAN5B_CLIP_VISION = "/models/wan/clip_vision_h.safetensors"
private const val WAN5B_VAE         = "/models/wan/Wan2.2_VAE.safetensors"
private const val WAN5B_INIT_IMAGE  = "../../app/app/src/commonMain/resources/media/input.png"

/**
 * Wan2.2-TI2V-5B image-to-video — the **fast tier** that fits a 12 GB GPU.
 *
 * Distinct from the A14B workflow ([wanImg2VidWorkflow]): TI2V-5B is a single dense model (~6 GB),
 * so there is no high-noise expert, no MoE LoRAs, and no `high_noise_sampler`. Runs in ~20 steps at
 * real CFG. (For ~4-step speed, drop in the FastWan 5B LoRA once it is on the server.)
 *
 * Node graph:
 *   sd.diffusion (TI2V-5B) ─┐
 *   sd.encoders (umt5 + clip_vision) ├─→ sd.model → sd.context → sd.img2vid → preview
 *   sd.vae (Wan2.2 VAE)    ─┘                  ↑
 *   sd.sampler (20-step, cfg 5) ───────────────┘
 */
internal val wan5bImg2VidWorkflow = WorkflowDefinition(
    id = "wan5b-img2vid",
    name = "Wan Image to Video (5B, fits 12GB)",
    nodes = listOf(
        guideNote(
            """
            Wan2.2-TI2V-5B Image → Video · fast tier (fits 12 GB)

            A dense 5B model that runs on a 12 GB card, unlike the A14B MoE
            pair (~25.8 GB) which needs a bigger GPU.

            Tip: set init_image on sd.img2vid. ~20 steps at cfg 5. Add the
            FastWan 5B LoRA to the loras port to cut steps to ~4.
            """,
        ),
        NodeRef(
            id = "sddiffusion",
            type = "sd.diffusion",
            config = mapOf(
                "diffusion_model_path" to WorkflowValue.StringValue(WAN5B_DIFFUSION),
            ),
        ),
        NodeRef(
            id = "encoders",
            type = "sd.encoders",
            config = mapOf(
                "t5xxl_path"       to WorkflowValue.StringValue(WAN5B_T5),
                "clip_vision_path" to WorkflowValue.StringValue(WAN5B_CLIP_VISION),
            ),
        ),
        NodeRef(
            id = "sdvae",
            type = "sd.vae",
            config = mapOf("vae_path" to WorkflowValue.StringValue(WAN5B_VAE)),
        ),
        NodeRef(id = "sdmodel", type = "sd.model"),
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
                "sample_method" to WorkflowValue.StringValue("euler"),
                "scheduler"     to WorkflowValue.StringValue("discrete"),
                "sample_steps"  to WorkflowValue.IntValue(20),
                "txt_cfg"       to WorkflowValue.DoubleValue(5.0),
                "flow_shift"    to WorkflowValue.DoubleValue(5.0),
            ),
        ),
        NodeRef(
            id = "img2vid",
            type = "sd.img2vid",
            config = mapOf(
                "init_image"   to WorkflowValue.StringValue(WAN5B_INIT_IMAGE),
                "prompt"       to WorkflowValue.StringValue("the scene comes alive, gentle camera push-in, natural motion"),
                "width"        to WorkflowValue.IntValue(480),
                "height"       to WorkflowValue.IntValue(480),
                // 17 frames fits the Wan VAE decode buffer on 12 GB; 49 OOMs the decode (even tiled).
                "video_frames" to WorkflowValue.IntValue(17),
                "fps"          to WorkflowValue.IntValue(16),
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
        ConnectionRef("sampler",     "sampler",   "img2vid", "sampler"),
        ConnectionRef("img2vid",     "frames",    "preview", "value"),
    ),
)
