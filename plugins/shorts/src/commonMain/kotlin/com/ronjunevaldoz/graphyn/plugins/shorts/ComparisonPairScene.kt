package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.booleanValue as b
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

// Same base Flux schnell checkpoint as imageMotionSceneSubgraph/mascotSubgraph — these are plain
// generations, not edits.
private const val COMPARISON_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val COMPARISON_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
private const val COMPARISON_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val COMPARISON_VAE = "/models/flux/vae/ae.safetensors"

/**
 * Generates ONE image for one side of a comparison pair — deliberately split out from
 * [comparisonLayoutMotionSubgraph] rather than bundled into one two-image subgraph. The free
 * boundary-port matching convention ([buildInputMap] in `WorkflowExecutionScheduling.kt`) resolves
 * an unconnected inner port purely by *name*, applied identically to every node in the subgraph —
 * two `promptEnhance`-shaped nodes in one subgraph would both expose an identical unconnected
 * "prompt" port and collide (both would receive the same externally-supplied value, with no way to
 * give image A and image B different prompts). Calling this subgraph twice at the outer workflow
 * level — once per side, each its own `NodeRef` — sidesteps the collision entirely: each instance's
 * `prompt`/`niche`/`visual_style` boundary ports are wired independently via outer `ConnectionRef`s.
 *
 * Ends with `preview.view` fed by an internal `media.image_import` (not the raw `sd.txt2img` path
 * directly) — every caller needs a real image handle, not a bare path, so the import step is done
 * once in here instead of being duplicated as a separate node at every call site (the same
 * consolidation applied to [mascotPointEditSubgraph], see its doc comment). Wrap with
 * [ShortsNodeTypes.SCENE_SUBGRAPH] at the call site — its wrapper executor relabels the free
 * `value` output to `video`, matching every other scene in this pipeline (despite this being an
 * image, not a video — Ken Burns happens once on the *composited* frame in
 * [comparisonLayoutMotionSubgraph], not per source photo, but the wrapper naming is uniform).
 */
public fun comparisonImageSubgraph(
    id: String,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Comparison Image",
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(COMPARISON_DIFFUSION))))
        add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(COMPARISON_CLIP_L), "t5xxl_path" to s(COMPARISON_T5XXL))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(COMPARISON_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf("diffusion_flash_attn" to b(true), "n_threads" to i(-1))))
        add(NodeRef("promptEnhance", ShortsConstants.PROMPT_ENHANCE_NODE_TYPE))
        add(NodeRef("sampler", "sd.sampler", config = mapOf(
            "sample_method" to s("euler"), "scheduler" to s("discrete"),
            "sample_steps" to i(4), "txt_cfg" to d(1.0), "distilled_guidance" to d(3.5), "flow_shift" to d(3.0),
        )))
        add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
            "negative_prompt" to s(""), "width" to i(width), "height" to i(height), "seed" to i(-1), "batch_count" to i(1),
        )))
        add(NodeRef("import", "media.image_import"))
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
        add(ConnectionRef("import", "image", "preview", "value"))
    },
)

/**
 * Composites two already-generated images + labels + the shared mascot pose into one still frame
 * (`media.comparison_layout`), then Ken-Burns that composite into a clip — the visual "motion" is a
 * light hold/zoom on the finished layout, not per-photo animation. `image_a`/`image_b`/`label_a`/
 * `label_b`/`mascot` are boundary ports (no collision risk here — one node, `layout`, owns all of
 * them, all distinctly named). Wrap with [ShortsNodeTypes.SCENE_SUBGRAPH] at the call site — its
 * wrapper executor relabels the free `value` output to `video`, matching every other scene in this
 * pipeline.
 *
 * `kenBurns.duration_ms` is left as a free boundary port (no config value) rather than a Kotlin
 * function parameter — the caller wires it from the real measured narration duration (divided across
 * pairs) so each clip's length actually matches how long the audio takes to say it, instead of a
 * fixed guess with no relationship to the narration's real pacing.
 *
 * [useKenBurns] toggles the zoom off (both `zoom_start`/`zoom_end` at 1.0 — a static hold) rather
 * than swapping in a different node: this format's motion is a light hold/zoom on the whole
 * composited frame, and a comparison-explainer's viewer attention is on reading the labels/captions,
 * not camera movement — unlike a photo-scene short, subtle zoom isn't load-bearing here, so it's a
 * caller preference, not a fixed default.
 */
public fun comparisonLayoutMotionSubgraph(
    id: String,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    useKenBurns: Boolean = true,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Comparison Layout + Motion",
    nodes = buildList {
        add(NodeRef("layout", "media.comparison_layout", config = mapOf(
            "style_config" to WorkflowValue.RecordValue(mapOf(
                "background_color" to s("#FFFFFF"),
                "label_font_family" to s("Helvetica Neue"),
                "label_font_size" to i(36),
                "label_color" to s("#000000"),
                "caption_font_family" to s("Helvetica Neue"),
                "caption_font_size" to i(44),
                "caption_color" to s("#000000"),
                "panel_gap" to i(24),
            )),
            "width" to i(width), "height" to i(height),
        )))
        add(NodeRef("kenBurns", "media.ken_burns", config = mapOf(
            "fps" to d(24.0),
            "zoom_start" to d(1.0), "zoom_end" to d(if (useKenBurns) 1.05 else 1.0),
            "pan_x" to s("center"), "pan_y" to s("center"),
            "width" to i(width), "height" to i(height),
        )))
        add(NodeRef("preview", "preview.view"))
    },
    connections = buildList {
        add(ConnectionRef("layout", "image", "kenBurns", "image"))
        add(ConnectionRef("kenBurns", "video", "preview", "value"))
    },
)
