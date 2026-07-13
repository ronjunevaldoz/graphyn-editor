package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.booleanValue as b
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

// The base mascot reference is a plain FLUX schnell generation, same checkpoint as
// characterSheetSubgraphDynamic/comparisonImageSubgraph — see fluxPlainGenerationSubgraph
// (FluxPlainGeneration.kt), which [mascotSubgraph] below now delegates to, for the shared
// FLUX_DIFFUSION/FLUX_CLIP_L/FLUX_T5XXL/FLUX_VAE constants (ImageMotionScene.kt).

/**
 * Default original mascot design, not tied to any specific existing character — a generic,
 * user-overridable starting point for [comparisonPairSubgraph]'s recurring reactor. Callers should
 * pass their own [mascotDescription] to [mascotSubgraph] for a distinctive result; this default
 * only exists so the comparison workflow is runnable out of the box.
 *
 * Neutral standing pose, arms relaxed at sides — deliberately generic and simple as the base
 * reference; all pose customization (pointing direction, etc.) happens entirely in the edit step
 * via [mascotPointEditSubgraph], not baked into this base description. A T-pose base was tried as
 * an experiment (both arms extended, on the theory that editing FROM an already-extended arm is a
 * smaller pose delta than lifting one from resting-at-the-side) — reverted: a real test showed the
 * model doesn't reliably produce an actual T-pose from this instruction anyway (one real attempt
 * came back as a side-profile pointing pose, not two arms extended), so it wasn't buying the
 * intended reliability benefit. Reverted to the simpler, more standard neutral pose.
 * No pointing language baked in here — pointing direction used to be baked into 3
 * independently-generated poses (same seed, different prompt text), confirmed via real output NOT
 * to produce a consistent character; pointing is added by editing this one base image instead.
 */
public const val DEFAULT_MASCOT_DESCRIPTION: String =
    "a simple, friendly round-bodied cartoon mascot character, minimalist flat design, solid " +
        "single-color body, large expressive eyes, no props, no clothing, standing pose, calm " +
        "neutral expression, arms relaxed at sides"

/**
 * FLUX Kontext edit instructions for [mascotPointEditSubgraph] — not generation prompts. Each
 * describes a targeted edit of the ONE base mascot reference from [mascotSubgraph] (a neutral
 * standing pose, see [DEFAULT_MASCOT_DESCRIPTION]), not a new character.
 *
 * Explicitly names the attributes to preserve (face, hairstyle, hair color, outfit, art style)
 * instead of a generic "everything else unchanged" — confirmed via real output that the vague
 * version let the text prompt's pose description dominate over the reference image's identity,
 * producing a different-looking character per direction despite both conditioning on the same
 * base image (see [mascotPointEditSubgraph]'s doc comment for the seed-pinning experiment that
 * ruled out seed as the fix). Naming the specific attributes gives the model concrete anchors to
 * hold onto instead of an underspecified "the same".
 *
 * Refers to "the arm on the left/right side of the frame" rather than the character's own
 * anatomical left/right arm — for a front-facing character those are mirrored (the character's
 * own left arm appears on the right side of the frame), and frame-relative phrasing is what
 * actually matches [POINT_LEFT]/[POINT_RIGHT]'s intended on-screen result.
 *
 * Also explicitly constrains the OTHER (non-pointing) arm to rest at the side — confirmed via real
 * output that leaving it unconstrained let the model free-associate a second gesture near the head
 * (a hand shape resembling a peace sign / bunny ears next to the face), a documented FLUX Kontext
 * failure mode when only one body part's target pose is specified. Per Black Forest Labs' own
 * Kontext prompting guide (docs.bfl.ml), unmentioned elements are the most likely to drift.
 */
public object MascotPointDirections {
    public const val POINT_LEFT: String =
        "Using the reference image, keep the exact same character: identical face, identical " +
            "hairstyle and hair color, identical outfit, identical art style. Change only the " +
            "pose: raise the arm on the left side of the frame and point with the index finger " +
            "toward the upper-left of the frame; keep the arm on the right side of the frame " +
            "resting down at the character's side, unchanged from the reference image."
    public const val POINT_RIGHT: String =
        "Using the reference image, keep the exact same character: identical face, identical " +
            "hairstyle and hair color, identical outfit, identical art style. Change only the " +
            "pose: raise the arm on the right side of the frame and point with the index finger " +
            "toward the upper-right of the frame; keep the arm on the left side of the frame " +
            "resting down at the character's side, unchanged from the reference image."
}

/**
 * Negative prompt for [mascotPointEditSubgraph] — pushes away from the real failure modes seen
 * in end-to-end output: (1) the model swapping in a different-looking character instead of
 * editing the reference's pose, (2) a degenerate multi-panel "sticker sheet" collage instead of
 * one clean character (a fixed seed produced disconnected face/arm fragments with stray
 * speech-bubble text for one direction's edit), and (3) an unconstrained second hand ending up
 * near the head/face (see [MascotPointDirections]'s doc comment) — "morphing"/"extra gesture" are
 * the terms a FLUX Kontext character-editing guide (selfielabstudio.com) specifically recommends
 * for blocking that third failure mode.
 */
private const val MASCOT_EDIT_NEGATIVE_PROMPT: String =
    "different character, different face, different hairstyle, different hair color, different " +
        "outfit, different art style, multiple characters, multiple panels, comic strip, sticker " +
        "sheet, collage, speech bubbles, text, watermark, disconnected limbs, extra limbs, extra " +
        "gesture, hand near face, hand near head, morphing, style shift, blurry, deformed"

/**
 * Generates the ONE base mascot reference image, once per short, before the per-pair loop.
 * [seed] is pinned by default for reproducibility of this base image (Kontext edits derived from
 * it via [mascotPointEditSubgraph] are what actually deliver cross-pair consistency, not the seed
 * alone — see [DEFAULT_MASCOT_DESCRIPTION]'s doc comment). Sibling to
 * [characterSheetSubgraphDynamic]; same shape, different subject (an illustrated mascot instead of
 * a photorealistic recurring character).
 *
 * [useLlmPromptEnhance] swaps the deterministic `media.prompt_enhance` (plain string-joining) for
 * [promptEnhanceLlmSpec] (a real Ollama call that genuinely expands the description) — opt-in, not
 * a default, since it adds a network call in front of every generation. See that file's doc
 * comment for why it exists and its fallback behavior.
 */
public fun mascotSubgraph(
    id: String,
    mascotDescription: String = DEFAULT_MASCOT_DESCRIPTION,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    seed: Int = 42,
    useLlmPromptEnhance: Boolean = false,
): WorkflowDefinition = fluxPlainGenerationSubgraph(
    id = id,
    name = "Mascot Base",
    width = width,
    height = height,
    prompt = mascotDescription,
    lighting = "soft even lighting, plain white background",
    details = "clean subject separation, single character only, no text, no props",
    seed = seed,
    useLlmPromptEnhance = useLlmPromptEnhance,
    // The base mascot's own raw path is consumed as sd.id_cond's ref_images by
    // mascotPointEditSubgraph — importing it here to a real image handle would break that cast, so
    // importResult stays false (the default).
)

/**
 * Edits the ONE base mascot reference (from [mascotSubgraph]) into a pointing pose via FLUX
 * Kontext, instead of generating each pose independently — the actual fix for cross-pair mascot
 * consistency (a same-seed-different-prompt attempt at this was confirmed NOT sufficient; only
 * conditioning on the same source image via `ref_images` reliably keeps the character consistent).
 *
 * Mirrors [imageMotionSceneSubgraphDynamic]'s `useCharacterSheet=true` branch, not
 * [imageMotionSceneEditSubgraph]: that function bakes `ref_images` into node config as a literal
 * path at Kotlin-build time, which doesn't fit here — the base mascot's real image path is only
 * known once that generation actually runs. `ref_images` is left as a free/unconnected boundary
 * port instead; the outer workflow wires the base mascot's raw image output into it at runtime
 * (`sd.id_cond`'s `ref_images` is list-typed, so a single incoming connection auto-wraps into a
 * one-element list — no separate list-building node needed).
 *
 * [seed] defaults to random (-1). A fixed shared seed across both direction edits was tried and
 * reverted: with `ref_images` correctly wired (see [ShortsSubgraphSpecs]'s `shortsSceneSubgraphSpec`
 * fix) Kontext does condition on the base reference, but pinning `seed = 42` for both edits did NOT
 * make them converge on the same character — confirmed via real output that "point right" rendered
 * a clean but still different-looking character, while "point left" at that same seed produced a
 * degenerate multi-panel sticker-sheet artifact (broken output, not just a different style) that
 * reproduced identically on every reuse of that generation. A specific seed can be out-of-distribution
 * for a given prompt with no way to predict that in advance, so pinning traded one open problem
 * (cross-direction identity drift) for a worse, less predictable one (seed-specific corruption).
 * Cross-direction identity consistency remains an open problem beyond what `ref_images` conditioning
 * alone delivers; within a single direction, the same generation is reused byte-for-byte across
 * every pair that needs it (see [comparisonPairSubgraph]'s call site), which is consistent today.
 */
public fun mascotPointEditSubgraph(
    id: String,
    editInstruction: String,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    seed: Int = -1,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Mascot Point Edit",
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(FLUX_KONTEXT_DIFFUSION))))
        add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(FLUX_CLIP_L), "t5xxl_path" to s(FLUX_T5XXL))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(FLUX_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf(
            "diffusion_flash_attn" to b(true),
            "n_threads" to i(-1),
            // Confirmed necessary for Kontext in DemoFluxKontextImg2ImgDef.kt.
            "clip_on_cpu" to b(true),
        )))
        // ref_images deliberately left unset — see this function's doc comment.
        add(NodeRef("idCond", "sd.id_cond"))
        add(NodeRef("sampler", "sd.sampler", config = mapOf(
            "sample_method" to s("euler"),
            "scheduler" to s("discrete"),
            // Kontext needs more steps than base Flux schnell's 4-step distillation.
            "sample_steps" to i(20),
            // txt_cfg maps directly to stable-diffusion.cpp's --cfg-scale (SdSamplerSpec.kt's own
            // port description). Under the standard CFG formula pred = uncond + scale*(cond-uncond),
            // a scale of 1.0 makes the negative-prompt branch's contribution cancel to exactly zero
            // — MASCOT_EDIT_NEGATIVE_PROMPT would be silently inert at the library's own documented
            // Kontext default of cfg-scale=1.0 (github.com/leejet/stable-diffusion.cpp/docs/kontext.md).
            // A first attempt raised this to 2.5; confirmed via real output that introduced a stray
            // hand/gesture artifact. Settled on 1.5 as a compromise.
            //
            // UPDATE (2026-07-11): 1.5 was NOT actually safe — a real end-to-end comparisonShortWorkflow
            // run hit FLUX Kontext's documented degenerate "sticker sheet" collage failure (disconnected
            // limb/face fragments, stray speech-bubble-like text; see MASCOT_EDIT_NEGATIVE_PROMPT's own
            // doc comment) at cfg=1.5, reproduced on a second independent seed at the same cfg (2/2).
            // Raising to 3.0 against the identical reference image + both failing seeds produced a
            // clean, correctly-posed, single-character result on the first try — consistent with a
            // broader pattern confirmed this session across two unrelated Kontext-edited characters:
            // this checkpoint's negative-prompt compliance genuinely needs cfg in the ~2.2-3.5 range,
            // not 1.0-1.5, to reliably suppress hallucinated content. The earlier cfg=2.5 stray-hand
            // finding may have been seed-specific rather than a property of that cfg value — not
            // re-verified, but even if real, a stray hand is a far smaller defect than a full collage.
            "txt_cfg" to d(3.0),
            "distilled_guidance" to d(3.5),
            "flow_shift" to d(3.0),
        )))
        add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
            "prompt" to s(editInstruction),
            "negative_prompt" to s(MASCOT_EDIT_NEGATIVE_PROMPT),
            "width" to i(width),
            "height" to i(height),
            "seed" to i(seed),
            "batch_count" to i(1),
        )))
        // Imports the generated path into a real image handle here rather than leaving it to the
        // caller — every caller of this subgraph needs the same conversion (comparisonPairSubgraph
        // consumes it as an image handle, not a raw path), so doing it once inside removes a
        // duplicate media.image_import node from every call site's own graph.
        add(NodeRef("import", "media.image_import"))
        add(NodeRef("preview", "preview.view"))
    },
    connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("encoders", "encoders", "model", "encoders"))
        add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "ctx", "model"))
        add(ConnectionRef("ctx", "context", "txt2img", "context"))
        add(ConnectionRef("sampler", "sampler", "txt2img", "sampler"))
        add(ConnectionRef("idCond", "id_cond", "txt2img", "id_cond"))
        add(ConnectionRef("txt2img", "image", "import", "path"))
        add(ConnectionRef("import", "image", "preview", "value"))
    },
)

/**
 * Mirrors an already-generated mascot pointing pose horizontally instead of running a second,
 * independent FLUX Kontext edit — e.g. turns a left-pointing mascot into a right-pointing one.
 * Caller wires the source direction's raw image output into this subgraph's free `image` port
 * (same free-boundary-port pattern as [mascotPointEditSubgraph]'s `ref_images`).
 *
 * Sidesteps two real problems FLUX Kontext showed for directional edits: (1) "left"/"right" in the
 * prompt is unreliable — both directions sometimes rendered pointing the same way regardless of
 * which word was used, even with explicit frame-relative phrasing: see [MascotPointDirections]'s
 * doc comment for the wording already used here to mitigate that, and (2) even when direction did
 * differ, it was a second independent Kontext sample that could still drift the character (a
 * documented open problem — see [mascotPointEditSubgraph]'s doc comment). A pixel-mirror of a
 * generation that's already confirmed correct guarantees byte-for-byte identity consistency between
 * directions and costs a fast ffmpeg call instead of a ~3 minute Kontext sample.
 */
public fun mascotFlipSubgraph(id: String): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Mascot Point Flip",
    nodes = listOf(
        // image deliberately left unset — see this function's doc comment.
        NodeRef("flip", "media.image_flip"),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("flip", "image", "preview", "value"),
    ),
)
