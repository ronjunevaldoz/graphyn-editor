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
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    imagePathSidecarPath: String? = null,
) = com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraph(
    prompt = prompt, niche = niche, imageCount = imageCount, visualStyle = visualStyle, character = character,
    width = width, height = height, imagePathSidecarPath = imagePathSidecarPath,
)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraphDynamic */
internal fun imageMotionSceneSubgraphDynamic(
    id: String,
    imageCount: Int = 2,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    useCharacterSheet: Boolean = false,
    imagePathSidecarPath: String? = null,
) = com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneSubgraphDynamic(
    id = id, imageCount = imageCount, width = width, height = height, useCharacterSheet = useCharacterSheet,
    imagePathSidecarPath = imagePathSidecarPath,
)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneEditSubgraph */
internal fun imageMotionSceneEditSubgraph(
    id: String,
    referenceImagePath: String,
    editInstruction: String,
    imageCount: Int = 2,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    imagePathSidecarPath: String? = null,
) = com.ronjunevaldoz.graphyn.plugins.shorts.imageMotionSceneEditSubgraph(
    id = id, referenceImagePath = referenceImagePath, editInstruction = editInstruction,
    imageCount = imageCount, width = width, height = height,
    imagePathSidecarPath = imagePathSidecarPath,
)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.characterSheetSubgraphDynamic */
internal fun characterSheetSubgraphDynamic(
    id: String,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    poseInstruction: String = com.ronjunevaldoz.graphyn.plugins.shorts.CharacterSheetPoses.NEUTRAL,
    expressionDetail: String = "",
) = com.ronjunevaldoz.graphyn.plugins.shorts.characterSheetSubgraphDynamic(
    id = id, width = width, height = height, poseInstruction = poseInstruction, expressionDetail = expressionDetail,
)

internal typealias CharacterSheetPoses = com.ronjunevaldoz.graphyn.plugins.shorts.CharacterSheetPoses

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.storyboardGeneratorSubgraph */
internal fun storyboardGeneratorSubgraph(topic: String) =
    com.ronjunevaldoz.graphyn.plugins.shorts.storyboardGeneratorSubgraph(topic)

internal const val COMPARISON_PAIR_COUNT = com.ronjunevaldoz.graphyn.plugins.shorts.COMPARISON_PAIR_COUNT
internal const val COMPARISON_FIELD_NODE_TYPE = ShortsNodeTypes.COMPARISON_FIELD
internal const val COMPARISON_PAIR_FIELD_NODE_TYPE = ShortsNodeTypes.COMPARISON_PAIR_FIELD
internal const val COMPARISON_CAPTIONS_NODE_TYPE = ShortsNodeTypes.COMPARISON_CAPTIONS
internal const val COMPARISON_PAIR_DURATION_NODE_TYPE = ShortsNodeTypes.COMPARISON_PAIR_DURATION

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.comparisonGeneratorSubgraph */
internal fun comparisonGeneratorSubgraph(topic: String) =
    com.ronjunevaldoz.graphyn.plugins.shorts.comparisonGeneratorSubgraph(topic)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.mascotSubgraph */
internal fun mascotSubgraph(
    id: String,
    mascotDescription: String = com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION,
    poseInstruction: String = com.ronjunevaldoz.graphyn.plugins.shorts.MascotPoses.NEUTRAL,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
) = com.ronjunevaldoz.graphyn.plugins.shorts.mascotSubgraph(
    id = id, mascotDescription = mascotDescription, poseInstruction = poseInstruction, width = width, height = height,
)

internal typealias MascotPoses = com.ronjunevaldoz.graphyn.plugins.shorts.MascotPoses

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.comparisonImageSubgraph */
internal fun comparisonImageSubgraph(id: String, width: Int = SHORTS_WIDTH, height: Int = SHORTS_HEIGHT) =
    com.ronjunevaldoz.graphyn.plugins.shorts.comparisonImageSubgraph(id = id, width = width, height = height)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.comparisonLayoutMotionSubgraph */
internal fun comparisonLayoutMotionSubgraph(
    id: String,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    useKenBurns: Boolean = true,
) = com.ronjunevaldoz.graphyn.plugins.shorts.comparisonLayoutMotionSubgraph(
    id = id, width = width, height = height, useKenBurns = useKenBurns,
)

/** @see com.ronjunevaldoz.graphyn.plugins.shorts.comparisonPairSubgraph */
internal fun comparisonPairSubgraph(
    id: String,
    pairIndex: Int,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
    useKenBurns: Boolean = true,
) = com.ronjunevaldoz.graphyn.plugins.shorts.comparisonPairSubgraph(
    id = id, pairIndex = pairIndex, width = width, height = height, useKenBurns = useKenBurns,
)
