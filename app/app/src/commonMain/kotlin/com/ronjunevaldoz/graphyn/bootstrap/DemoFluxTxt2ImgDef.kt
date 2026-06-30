package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Default model paths — override via config port values at runtime.
private const val FLUX_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val FLUX_CLIP_L    = "/models/flux/text_encoder/clip_l.safetensors"
private const val FLUX_T5XXL     = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val FLUX_VAE       = "/models/flux/vae/ae.safetensors"

/**
 * FLUX.1-schnell text-to-image workflow.
 *
 * Node graph:
 *   sd.context (model + hardware)
 *       ↓ context
 *   sd.sampler (4-step euler, distilled guidance)
 *       ↓ sampler
 *   sd.txt2img (prompt + dimensions)
 *       ↓ images[0]
 *   preview.view
 *
 * FLUX.1-schnell is a 4-step distilled model:
 *   - txt_cfg = 1.0  (CFG is irrelevant for distilled; keep at 1)
 *   - distilled_guidance = 3.5
 *   - flow_shift = 3.0
 *   - negative_prompt = "" (ignored by distilled models)
 */
internal val fluxTxt2ImgWorkflow = WorkflowDefinition(
    id = "flux-txt2img",
    name = "FLUX Text to Image",
    nodes = listOf(
        guideNote(
            """
            FLUX.1-schnell Text → Image

            Generates a 1024×1024 image from a text prompt using
            FLUX.1-schnell (4-step distilled, no negative prompt needed).

            Flow: SD Context → SD Sampler → SD Text→Image → Preview

            Tip: edit the prompt port on sd.txt2img to change the subject.
            Change width/height for different aspect ratios (multiples of 16).
            """,
        ),
        NodeRef(
            id = "ctx",
            type = "sd.context",
            config = mapOf(
                "diffusion_model_path" to WorkflowValue.StringValue(FLUX_DIFFUSION),
                "clip_l_path"          to WorkflowValue.StringValue(FLUX_CLIP_L),
                "t5xxl_path"           to WorkflowValue.StringValue(FLUX_T5XXL),
                "vae_path"             to WorkflowValue.StringValue(FLUX_VAE),
                "diffusion_flash_attn" to WorkflowValue.BooleanValue(true),
                "n_threads"            to WorkflowValue.IntValue(-1),
            ),
        ),
        NodeRef(
            id = "sampler",
            type = "sd.sampler",
            config = mapOf(
                "sample_method"       to WorkflowValue.StringValue("euler"),
                "scheduler"           to WorkflowValue.StringValue("discrete"),
                "sample_steps"        to WorkflowValue.IntValue(4),
                "txt_cfg"             to WorkflowValue.DoubleValue(1.0),
                "distilled_guidance"  to WorkflowValue.DoubleValue(3.5),
                "flow_shift"          to WorkflowValue.DoubleValue(3.0),
            ),
        ),
        NodeRef(
            id = "txt2img",
            type = "sd.txt2img",
            config = mapOf(
                "prompt"          to WorkflowValue.StringValue("a cat sitting on a windowsill, golden hour, photorealistic"),
                "negative_prompt" to WorkflowValue.StringValue(""),
                "width"           to WorkflowValue.IntValue(1024),
                "height"          to WorkflowValue.IntValue(1024),
                "seed"            to WorkflowValue.IntValue(-1),
                "batch_count"     to WorkflowValue.IntValue(1),
            ),
        ),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("ctx",     "context", "txt2img", "context"),
        ConnectionRef("sampler", "sampler", "txt2img", "sampler"),
        ConnectionRef("txt2img", "images",  "preview", "value"),
    ),
)
