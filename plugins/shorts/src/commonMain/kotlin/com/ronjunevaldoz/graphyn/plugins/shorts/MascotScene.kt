package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Same base Flux schnell checkpoint as characterSheetSubgraphDynamic — a mascot illustration is a
// plain generation, not an edit, so it doesn't need FLUX Kontext's extra sampling steps or
// ref-image conditioning overhead just to produce the reference itself.
private const val MASCOT_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val MASCOT_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
private const val MASCOT_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val MASCOT_VAE = "/models/flux/vae/ae.safetensors"

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)
private fun b(value: Boolean) = WorkflowValue.BooleanValue(value)

/**
 * Default original mascot design, not tied to any specific existing character — a generic,
 * user-overridable starting point for [comparisonPairSubgraph]'s recurring reactor. Callers should
 * pass their own [mascotDescription] to [mascotSubgraph] for a distinctive result; this default
 * only exists so the comparison workflow is runnable out of the box.
 */
public const val DEFAULT_MASCOT_DESCRIPTION: String =
    "a simple, friendly round-bodied cartoon mascot character, minimalist flat design, solid " +
        "single color body, large expressive eyes, no other distinguishing props or clothing"

/**
 * Pose/expression instructions for [mascotSubgraph] — call the subgraph once per variant (each
 * becomes its own outer node) exactly like [CharacterSheetPoses]/[characterSheetSubgraphDynamic],
 * generating a small fixed set of reusable poses once per short instead of once per pair.
 */
public object MascotPoses {
    public const val NEUTRAL: String = "standing pose, calm neutral expression, facing forward"
    public const val CONFUSED: String = "one hand near chin, puzzled expression, tilted head, question-mark feel"
    public const val EXPLAINING: String = "one arm raised pointing outward, confident expression, mid-explanation gesture"
}

/**
 * Generates ONE mascot pose image, once per short, before the per-pair loop — not per-pair.
 * Sibling to [characterSheetSubgraphDynamic]; same shape, different subject (an illustrated
 * mascot instead of a photorealistic recurring character), and no `ref_images` conditioning
 * output needed — [comparisonPairSubgraph] uses the raw pose image directly via
 * `media.image_import`, it doesn't feed FLUX Kontext.
 */
public fun mascotSubgraph(
    id: String,
    mascotDescription: String = DEFAULT_MASCOT_DESCRIPTION,
    poseInstruction: String = MascotPoses.NEUTRAL,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Mascot Pose",
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(MASCOT_DIFFUSION))))
        add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(MASCOT_CLIP_L), "t5xxl_path" to s(MASCOT_T5XXL))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(MASCOT_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf("diffusion_flash_attn" to b(true), "n_threads" to i(-1))))
        add(NodeRef("promptEnhance", ShortsConstants.PROMPT_ENHANCE_NODE_TYPE, config = mapOf(
            "prompt" to s(mascotDescription),
            "framing" to s(poseInstruction),
            "lighting" to s("soft even lighting, plain white background"),
            "details" to s("clean subject separation, single character only, no text, no props"),
        )))
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
            "width" to i(width),
            "height" to i(height),
            "seed" to i(-1),
            "batch_count" to i(1),
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
        // SHORTS_SCENE_SUBGRAPH_NODE_TYPE's wrapper executor reads the subgraph's free output by
        // the fixed key "value" (see comparisonImageSubgraph for the same pattern) — without this
        // terminal passthrough, the free output stays keyed "image" (txt2img's own port name) and
        // the wrapper silently resolves to NullValue instead of throwing, which is exactly what
        // broke mascotSave/mascotImport in the first real end-to-end run.
        add(ConnectionRef("txt2img", "image", "preview", "value"))
    },
)
