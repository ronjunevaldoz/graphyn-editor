package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.shorts.ShortsConstants
import com.ronjunevaldoz.graphyn.plugins.shorts.ShortsNodeTypes

// Bridges the desktop demo bootstrap to the published :plugins:shorts module. The reusable
// storyboard/scene/composition builders, executors, and node-type constants now live there; these
// aliases keep the desktop-only wiring (guide notes, launcher catalog, hardcoded topics) in this
// package unchanged while sourcing everything reusable from the library.

internal const val SHORTS_WIDTH = ShortsConstants.WIDTH
internal const val SHORTS_HEIGHT = ShortsConstants.HEIGHT
internal const val SUBGRAPH_CATEGORY = ShortsConstants.CATEGORY
internal const val CAPTION_STYLE_NODE_TYPE = ShortsConstants.CAPTION_STYLE_NODE_TYPE
internal const val PROMPT_ENHANCE_NODE_TYPE = ShortsConstants.PROMPT_ENHANCE_NODE_TYPE
internal val CAPTION_STYLE_DEFAULTS: Map<String, WorkflowValue> = ShortsConstants.CAPTION_STYLE_DEFAULTS

internal const val SHORTS_SCENE_SUBGRAPH_NODE_TYPE = ShortsNodeTypes.SCENE_SUBGRAPH
internal const val SHORTS_BATCH_SUBGRAPH_NODE_TYPE = ShortsNodeTypes.BATCH_SUBGRAPH
internal const val STORYBOARD_SUBGRAPH_NODE_TYPE = ShortsNodeTypes.STORYBOARD_SUBGRAPH
internal const val STORYBOARD_FIELD_NODE_TYPE = ShortsNodeTypes.STORYBOARD_FIELD
internal const val STORYBOARD_SCENE_FIELD_NODE_TYPE = ShortsNodeTypes.STORYBOARD_SCENE_FIELD
internal const val STORYBOARD_CAPTIONS_NODE_TYPE = ShortsNodeTypes.STORYBOARD_CAPTIONS
internal const val OLLAMA_UNLOAD_NODE_TYPE = ShortsNodeTypes.OLLAMA_UNLOAD

/** Builds the shared caption-style node with the pipeline defaults. */
internal fun shortsCaptionStyleNode(overrides: Map<String, WorkflowValue> = emptyMap()): NodeRef =
    com.ronjunevaldoz.graphyn.plugins.shorts.shortsCaptionStyleNode(overrides)

internal const val STORYBOARD_SCENE_COUNT = com.ronjunevaldoz.graphyn.plugins.shorts.STORYBOARD_SCENE_COUNT

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraph */
internal fun imageMotionSceneSubgraph(
    prompt: String,
    niche: String,
    imageCount: Int = 2,
    visualStyle: String = "",
    character: String = "",
) = com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraph(
    prompt = prompt, niche = niche, imageCount = imageCount, visualStyle = visualStyle, character = character,
)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraphDynamic */
internal fun imageMotionSceneSubgraphDynamic(id: String, imageCount: Int = 2) =
    com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraphDynamic(id = id, imageCount = imageCount)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.storyboardGeneratorSubgraph */
internal fun storyboardGeneratorSubgraph(topic: String) =
    com.ronjunevaldoz.graphyn.plugins.shorts.storyboardGeneratorSubgraph(topic)
