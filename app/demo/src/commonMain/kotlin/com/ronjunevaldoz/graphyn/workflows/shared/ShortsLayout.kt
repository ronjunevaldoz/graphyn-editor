package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition

private fun p(x: Int, y: Int) = WorkflowNodePosition(x, y)

internal fun shortsNodePositions() = mapOf(
    "guide" to p(40, 40),
    "outlineSource" to p(380, 40),
    "outline" to p(760, 40),
    "scenes" to p(1140, 40),
    "captions" to p(1520, 40),
    "scene1" to p(900, 40),
    "scene2" to p(900, 260),
    "scene3" to p(900, 480),
    "scene4" to p(900, 700),
    "scene5" to p(900, 920),
    "scene6" to p(900, 1140),
    "scene7" to p(900, 1360),
    "scene8" to p(900, 1580),
    "batch1" to p(1260, 120),
    "batch2" to p(1260, 940),
    "finalBatch" to p(1620, 540),
    "captionStyle" to p(1620, 760),
    "captionOverlay" to p(1980, 540),
    "encode" to p(2340, 540),
    "output" to p(2700, 540),
)

internal fun shortsSceneNodePositions(useImageMotion: Boolean) = mapOf(
    "diffusion" to p(40, 40),
    "encoders" to p(40, 240),
    "vae" to p(40, 440),
    "model" to p(380, 160),
    "ctx" to p(380, 360),
    "sampler" to p(380, 560),
    "promptEnhance" to p(760, 120),
    "scenePrompt" to p(380, 120),
    if (useImageMotion) "txt2img" to p(1140, 120) else "txt2vid" to p(1140, 120),
    if (useImageMotion) "import" to p(1520, 120) else "wrap" to p(1520, 120),
    if (useImageMotion) "frames" to p(1900, 120) else "sequence" to p(1900, 120),
    "preview" to p(2280, 120),
)

internal fun shortsBatchNodePositions() = mapOf(
    "clips" to p(40, 40),
    "stitch" to p(420, 40),
)
