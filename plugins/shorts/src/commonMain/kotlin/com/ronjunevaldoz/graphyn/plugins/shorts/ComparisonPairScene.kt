package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

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
 * Delegates to [fluxPlainGenerationSubgraph] with `importResult = true` (every caller needs a real
 * image handle, not a bare path — see that function's doc comment) and `prompt = null` (this
 * subgraph's `prompt` stays a free boundary port, resolved per-call from the outer pair's
 * runtime-extracted `promptA`/`promptB`, not a Kotlin-time literal like [mascotSubgraph]'s). Wrap
 * with [ShortsNodeTypes.SCENE_SUBGRAPH] at the call site — its wrapper executor relabels the free
 * `value` output to `video`, matching every other scene in this pipeline (despite this being an
 * image, not a video — Ken Burns happens once on the *composited* frame in
 * [comparisonLayoutMotionSubgraph], not per source photo, but the wrapper naming is uniform).
 */
public fun comparisonImageSubgraph(
    id: String,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
): WorkflowDefinition = fluxPlainGenerationSubgraph(
    id = id,
    name = "Comparison Image",
    width = width,
    height = height,
    importResult = true,
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
