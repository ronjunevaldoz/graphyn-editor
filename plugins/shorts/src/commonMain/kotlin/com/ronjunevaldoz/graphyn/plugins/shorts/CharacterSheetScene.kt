package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.booleanValue as b
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

// Same base Flux schnell checkpoint as imageMotionSceneSubgraph — a character portrait is a plain
// generation, not an edit, so it doesn't need FLUX Kontext's extra sampling steps or ref-image
// conditioning overhead just to produce the reference itself.
private const val CHARACTER_SHEET_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val CHARACTER_SHEET_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
private const val CHARACTER_SHEET_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val CHARACTER_SHEET_VAE = "/models/flux/vae/ae.safetensors"

/**
 * Preset pose/expression instructions for [characterSheetSubgraphDynamic] — call the subgraph
 * once per variant (each becomes its own outer node) to build a multi-reference set instead of
 * one static portrait. A single grid image with multiple poses would NOT work here: FLUX Kontext
 * conditions on one reference image as a whole and has no way to address "just this panel," so a
 * composite would likely confuse it rather than help — separate reference images, each wired into
 * the same `ref_images` list port (which auto-collects every incoming connection, see
 * `WorkflowExecutionScheduling.kt`'s `buildInputMap`), is the supported multi-reference shape.
 */
public object CharacterSheetPoses {
    public const val NEUTRAL: String = "front-facing portrait, centered, plain neutral background"
    public const val SMILING: String = "front-facing portrait, warm genuine smile, centered, plain neutral background"
    public const val ACTION: String = "three-quarter view, dynamic candid pose, mid-motion, plain neutral background"
}

/**
 * Generates ONE reference image of the storyboard's `character` field with a given
 * [poseInstruction]/[expressionDetail], once per short, before the per-scene loop — not
 * per-scene. Call this multiple times with different [CharacterSheetPoses] (each call is its own
 * outer `NodeRef`) to build a multi-reference set: every instance's free `image` output (the
 * still-connected [ShortsConstants.PROMPT_ENHANCE_NODE_TYPE] → `sd.txt2img` chain's raw `image`
 * port; no `media.image_import` step here, so it stays unconsumed and surfaces as this
 * subgraph's boundary output) can be wired into the same scene's `ref_images` list port.
 *
 * This feeds each scene's FLUX Kontext `ref_images` conditioning input — see
 * `imageMotionSceneSubgraphDynamic`'s `useCharacterSheet` path — so the character stays visually
 * anchored to one or more references instead of relying on the text description alone landing
 * similarly by chance across independent Flux samples.
 *
 * `character` is left as an unconnected boundary port on the inner `promptEnhance` node — same
 * free-matching convention [imageMotionSceneSubgraphDynamic] already uses — so the storyboard's
 * generated `character` field can be wired in from the outer workflow.
 */
public fun characterSheetSubgraphDynamic(
    id: String,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    poseInstruction: String = CharacterSheetPoses.NEUTRAL,
    expressionDetail: String = "",
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Character Sheet",
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(CHARACTER_SHEET_DIFFUSION))))
        add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(CHARACTER_SHEET_CLIP_L), "t5xxl_path" to s(CHARACTER_SHEET_T5XXL))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(CHARACTER_SHEET_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf("diffusion_flash_attn" to b(true), "n_threads" to i(-1))))
        add(NodeRef("promptEnhance", ShortsConstants.PROMPT_ENHANCE_NODE_TYPE, config = mapOf(
            "prompt" to s("character reference sheet, full visible face and outfit"),
            "framing" to s(poseInstruction),
            "lighting" to s("soft studio lighting"),
            "details" to s(
                listOf(expressionDetail, "high detail, clean subject separation, single character only, no other people")
                    .filter(String::isNotBlank).joinToString(", "),
            ),
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
    },
)
