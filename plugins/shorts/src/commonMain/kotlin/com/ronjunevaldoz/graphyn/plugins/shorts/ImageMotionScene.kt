package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private const val FLUX_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val FLUX_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
// Q3_K_S instead of Q5_K_M — the larger quant left almost no VRAM headroom for inference on a
// 12GB card once combined with a Flux diffusion checkpoint's own weights.
private const val FLUX_T5XXL = "/models/flux/text_encoder/t5-v1_1-xxl-encoder-Q3_K_S.gguf"
private const val FLUX_VAE = "/models/flux/vae/ae.safetensors"

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)
private fun b(value: Boolean) = WorkflowValue.BooleanValue(value)

/**
 * One reusable image-motion scene: Flux txt2img (niche-aware prompt) rendered into an
 * [imageCount]-second clip via a Ken Burns pan/zoom instead of a flat static repeat. Call this
 * once per scene in a shorts template — it's a plain builder function, not a shared runtime
 * instance, so each call gets its own prompt.
 */
public fun imageMotionSceneSubgraph(
    prompt: String,
    niche: String,
    imageCount: Int = 2,
    visualStyle: String = "",
    character: String = "",
): WorkflowDefinition = imageMotionSceneSubgraphInternal(
    id = "image-motion-scene-${prompt.hashCode()}",
    imageCount = imageCount,
    promptEnhanceConfig = mapOf(
        "prompt" to s(prompt), "niche" to s(niche), "visual_style" to s(visualStyle), "character" to s(character),
    ),
)

/**
 * Same scene pipeline, but `prompt`/`niche` are left as unconnected boundary ports on the inner
 * `promptEnhance` node instead of literal config — the execution engine free-matches whatever the
 * outer subgraph instance receives on ports of the same name. Use this when a storyboard generator
 * supplies prompts/niche at runtime instead of Kotlin-build-time literals.
 */
public fun imageMotionSceneSubgraphDynamic(id: String, imageCount: Int = 2): WorkflowDefinition =
    imageMotionSceneSubgraphInternal(id = id, imageCount = imageCount, promptEnhanceConfig = emptyMap())

private fun imageMotionSceneSubgraphInternal(
    id: String,
    imageCount: Int,
    promptEnhanceConfig: Map<String, WorkflowValue>,
): WorkflowDefinition {
    require(imageCount >= 1) { "Scene duration must be at least 1 second, got $imageCount" }
    return WorkflowDefinition(
        id = id,
        name = "Image Motion Scene",
        nodes = buildList {
            add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(FLUX_DIFFUSION))))
            add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(FLUX_CLIP_L), "t5xxl_path" to s(FLUX_T5XXL))))
            add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(FLUX_VAE))))
            add(NodeRef("model", "sd.model"))
            add(NodeRef("ctx", "sd.context", config = mapOf("diffusion_flash_attn" to b(true), "n_threads" to i(-1))))
            add(NodeRef("promptEnhance", ShortsConstants.PROMPT_ENHANCE_NODE_TYPE, config = promptEnhanceConfig))
            add(NodeRef("sampler", "sd.sampler", config = mapOf(
                "sample_method" to s("euler"),
                "scheduler" to s("discrete"),
                "sample_steps" to i(4),
                "txt_cfg" to d(1.0),
                "distilled_guidance" to d(3.5),
                "flow_shift" to d(3.0),
            )))
            add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
                "negative_prompt" to s(""),
                "width" to i(ShortsConstants.WIDTH),
                "height" to i(ShortsConstants.HEIGHT),
                "seed" to i(-1),
                "batch_count" to i(1),
            )))
            add(NodeRef("import", "media.image_import"))
            add(NodeRef("kenBurns", "media.ken_burns", config = mapOf(
                "duration_ms" to d(imageCount * 1000.0),
                "fps" to d(24.0),
                "zoom_start" to d(1.0),
                "zoom_end" to d(1.15),
                "pan_x" to s("center"),
                "pan_y" to s("center"),
                "width" to i(ShortsConstants.WIDTH),
                "height" to i(ShortsConstants.HEIGHT),
            )))
            add(NodeRef("preview", "preview.view"))
        },
        connections = buildList {
            add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
            add(ConnectionRef("encoders", "encoders", "model", "encoders"))
            add(ConnectionRef("vae", "vae", "model", "vae"))
            add(ConnectionRef("model", "model", "ctx", "model"))
            add(ConnectionRef("promptEnhance", "prompt", "txt2img", "prompt"))
            add(ConnectionRef("promptEnhance", "negative_prompt", "txt2img", "negative_prompt"))
            add(ConnectionRef("ctx", "context", "txt2img", "context"))
            add(ConnectionRef("sampler", "sampler", "txt2img", "sampler"))
            add(ConnectionRef("txt2img", "image", "import", "path"))
            add(ConnectionRef("import", "image", "kenBurns", "image"))
            add(ConnectionRef("kenBurns", "video", "preview", "value"))
        },
    )
}
