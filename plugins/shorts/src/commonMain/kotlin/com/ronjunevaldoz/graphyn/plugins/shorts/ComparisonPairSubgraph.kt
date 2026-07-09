package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

/**
 * Builds one comparison pair once so the outer workflow can loop without repeating the same
 * image-generation, import, and layout wiring four times.
 */
public fun comparisonPairSubgraph(
    id: String,
    pairIndex: Int,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    useKenBurns: Boolean = true,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Comparison Pair",
    nodes = buildList {
        add(NodeRef("niche", ShortsNodeTypes.COMPARISON_FIELD, config = mapOf("field" to s("niche"))))
        add(NodeRef("visualStyle", ShortsNodeTypes.COMPARISON_FIELD, config = mapOf("field" to s("visual_style"))))
        add(NodeRef("labelA", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("label_a"))))
        add(NodeRef("labelB", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("label_b"))))
        add(NodeRef("promptA", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("prompt_a"))))
        add(NodeRef("promptB", ShortsNodeTypes.COMPARISON_PAIR_FIELD, config = mapOf("index" to i(pairIndex), "field" to s("prompt_b"))))
        add(NodeRef("imageA", ShortsNodeTypes.SCENE_SUBGRAPH, subgraph = comparisonImageSubgraph(id = "$id-a", width = width, height = height)))
        add(NodeRef("imageB", ShortsNodeTypes.SCENE_SUBGRAPH, subgraph = comparisonImageSubgraph(id = "$id-b", width = width, height = height)))
        add(NodeRef("imageAImport", "media.image_import"))
        add(NodeRef("imageBImport", "media.image_import"))
        add(NodeRef("layout", ShortsNodeTypes.SCENE_SUBGRAPH, subgraph = comparisonLayoutMotionSubgraph(id = "$id-layout", width = width, height = height, useKenBurns = useKenBurns)))
    },
    connections = buildList {
        add(ConnectionRef("input", "value", "niche", "input"))
        add(ConnectionRef("input", "value", "visualStyle", "input"))
        add(ConnectionRef("input", "value", "labelA", "input"))
        add(ConnectionRef("input", "value", "labelB", "input"))
        add(ConnectionRef("input", "value", "promptA", "input"))
        add(ConnectionRef("input", "value", "promptB", "input"))
        add(ConnectionRef("input", "value", "layout", "input"))
        add(ConnectionRef("gate", "value", "imageA", "gate"))
        add(ConnectionRef("imageA", "video", "imageB", "gate"))
        add(ConnectionRef("imageA", "video", "imageAImport", "path"))
        add(ConnectionRef("imageB", "video", "imageBImport", "path"))
        add(ConnectionRef("niche", "result", "imageA", "niche"))
        add(ConnectionRef("niche", "result", "imageB", "niche"))
        add(ConnectionRef("visualStyle", "result", "imageA", "visual_style"))
        add(ConnectionRef("visualStyle", "result", "imageB", "visual_style"))
        add(ConnectionRef("promptA", "result", "imageA", "prompt"))
        add(ConnectionRef("promptB", "result", "imageB", "prompt"))
        add(ConnectionRef("imageAImport", "image", "layout", "image_a"))
        add(ConnectionRef("imageBImport", "image", "layout", "image_b"))
        add(ConnectionRef("labelA", "result", "layout", "label_a"))
        add(ConnectionRef("labelB", "result", "layout", "label_b"))
        add(ConnectionRef("mascot", "value", "layout", "mascot"))
        add(ConnectionRef("duration", "result", "layout", "duration_ms"))
    },
)
