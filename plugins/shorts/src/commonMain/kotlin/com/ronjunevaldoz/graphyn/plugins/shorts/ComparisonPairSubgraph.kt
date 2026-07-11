package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

/**
 * Builds one comparison pair once so the outer workflow can loop without repeating the same
 * image-generation, import, and layout wiring four times.
 *
 * `niche`/`visual_style` are NOT extracted in here (unlike `labelA`/`labelB`/`promptA`/`promptB`,
 * which genuinely differ per [pairIndex]) — niche/visual_style are the *same* value across every
 * pair, so re-extracting them 4 times (once per pair instance) was pure duplication. The caller
 * extracts them once and wires them into this subgraph node's own `niche`/`visual_style` ports;
 * `imageA`/`imageB`'s same-named inner ports are left unconnected here and resolve via the
 * free-boundary-port fallback (`buildInputMap`'s `externalInputs`, `WorkflowExecutionScheduling.kt`)
 * — the same mechanism already used for `gate`/`mascot`/`duration_ms` below, just for two more
 * port names. See [comparisonShortNodes]/[comparisonShortConnections] in the app module for the
 * single extraction + fan-out.
 *
 * [outputPath], when set, adds an internal `media.video_encode` that persists this pair's own
 * clip — the same debug-inspection artifact the outer workflow used to produce via its own
 * top-level `pairNSave` node per pair. Moved in here instead: "persist my own output" is this
 * subgraph's own concern, not the outer pipeline's, and it removes four near-identical nodes from
 * the outer graph's top level (confirmed a real source of visual clutter, not just an auto-layout
 * artifact — real end-to-end runs used these saved per-pair clips throughout this mascot
 * investigation, so the capability is kept, just relocated to where it logically belongs).
 */
public fun comparisonPairSubgraph(
    id: String,
    pairIndex: Int,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    useKenBurns: Boolean = true,
    outputPath: String? = null,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Comparison Pair",
    nodes = buildList {
        add(NodeRef("labelA", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("label_a"))))
        add(NodeRef("labelB", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("label_b"))))
        add(NodeRef("promptA", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("prompt_a"))))
        add(NodeRef("promptB", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("prompt_b"))))
        // comparisonImageSubgraph now imports its own raw path into a real image handle
        // internally (see its own doc comment) — no separate top-level Import node needed here.
        add(NodeRef("imageA", ShortsNodeTypes.SCENE_SUBGRAPH, subgraph = comparisonImageSubgraph(id = "$id-a", width = width, height = height)))
        add(NodeRef("imageB", ShortsNodeTypes.SCENE_SUBGRAPH, subgraph = comparisonImageSubgraph(id = "$id-b", width = width, height = height)))
        add(NodeRef("layout", ShortsNodeTypes.SCENE_SUBGRAPH, subgraph = comparisonLayoutMotionSubgraph(id = "$id-layout", width = width, height = height, useKenBurns = useKenBurns)))
        add(NodeRef("preview", "preview.view"))
        if (outputPath != null) {
            add(NodeRef("save", "media.video_encode", config = mapOf(
                "output_path" to s(outputPath), "bitrate" to s("medium"), "codec" to s("h264"),
            )))
        }
    },
    connections = buildList {
        // labelA/labelB/promptA/promptB's "input" port, imageA/imageB's "niche"/"visual_style"
        // ports, imageA's "gate" port, and layout's "mascot"/"duration_ms" ports are deliberately
        // left unconnected here — they resolve via the free-boundary-port fallback (buildInputMap's
        // externalInputs), which fills an unconnected inner port by name from this subgraph node's
        // own resolved inputs at the outer level. Wiring them to a literal ConnectionRef with a
        // source id like "input" or "gate" doesn't work: that id isn't a real node in this
        // subgraph's `nodes` list, so topologicalLayers() counts the edge as permanently
        // unsatisfiable incoming, which trips its cycle check. Confirmed via a real end-to-end run
        // (pair0 failed with "Workflow contains a cycle") before those lines were removed.
        add(ConnectionRef("imageA", "video", "imageB", "gate"))
        add(ConnectionRef("promptA", "result", "imageA", "prompt"))
        add(ConnectionRef("promptB", "result", "imageB", "prompt"))
        add(ConnectionRef("imageA", "video", "layout", "image_a"))
        add(ConnectionRef("imageB", "video", "layout", "image_b"))
        add(ConnectionRef("labelA", "result", "layout", "label_a"))
        add(ConnectionRef("labelB", "result", "layout", "label_b"))
        // "layout" is itself a SCENE_SUBGRAPH node, so its own output already arrives keyed "video"
        // (that wrapper's executor relabels its inner "value" free output to "video" — see
        // ShortsPlugin.kt). Without this terminal pass-through, this subgraph's free output would
        // stay keyed "video" instead of "value", and the *outer* SCENE_SUBGRAPH wrapper around this
        // whole subgraph (pair0..pair3) specifically reads inputs["value"] to relabel — finding
        // nothing, it silently returned NullValue. Confirmed via a real end-to-end run: pair0..pair3
        // reported Success but pairNSave failed "Expected a video media handle" on every pair.
        add(ConnectionRef("layout", "video", "preview", "value"))
        if (outputPath != null) add(ConnectionRef("layout", "video", "save", "video"))
    },
)
