package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION
import com.ronjunevaldoz.graphyn.plugins.shorts.MascotPointDirections

/**
 * Generates the mascot base + one Kontext-edited pointing direction, then mirrors it for the other
 * direction — nothing else (no photos, no narration, no stitching). FLUX Kontext at this checkpoint
 * has real, confirmed run-to-run output variance (see MascotScene.kt's mascotPointEditSubgraph doc
 * comment) that isn't cheaply auto-detectable without a vision-quality check this pipeline doesn't
 * have, so the practical mitigation is making a bad draw cheap to notice and reroll: this workflow
 * costs ~1-2 minutes (one base generation + one 20-step Kontext edit + one fast ffmpeg mirror)
 * instead of the full comparisonShortWorkflow's ~13 minutes, so a human can check the mascot and
 * rerun (fresh random seed=-1 each time) until it looks right before spending the full pipeline's
 * time on photos/narration/stitching that were never the unreliable part. Mirroring the right
 * direction instead of generating it independently also matches comparisonMascotNodes() in
 * DemoComparisonShortBuilders.kt — see mascotFlipSubgraph's doc comment for why.
 */
internal fun mascotPreviewWorkflow(
    mascotDescription: String? = null,
    width: Int? = null,
    height: Int? = null,
): WorkflowDefinition {
    val mascotDescription = mascotDescription ?: DEFAULT_MASCOT_DESCRIPTION
    val width = width ?: SHORTS_WIDTH
    val height = height ?: SHORTS_HEIGHT
    return WorkflowDefinition(
        id = "mascot-preview",
        name = "Mascot Preview (base + both directions)",
        nodes = buildList {
            add(NodeRef(
                "mascotBase", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotSubgraph(id = "mascot-preview-base", mascotDescription = mascotDescription, width = width, height = height),
            ))
            add(NodeRef(
                "mascotLeft", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotPointEditSubgraph(id = "mascot-preview-left", editInstruction = MascotPointDirections.POINT_LEFT, width = width, height = height),
            ))
            add(NodeRef(
                "mascotRight", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotFlipSubgraph(id = "mascot-preview-right-flip"),
            ))
            add(NodeRef("leftOutput", "media.file_output"))
            add(NodeRef("rightOutput", "media.file_output"))
        },
        connections = buildList {
            // Same shape as comparisonMascotNodes() in DemoComparisonShortBuilders.kt — one base, the
            // left direction edited and gated against it, the right direction a mirror of mascotLeft's
            // own output (its "image" connection is itself the ordering dependency; no gate needed).
            add(ConnectionRef("mascotBase", "video", "mascotLeft", "ref_images"))
            add(ConnectionRef("mascotBase", "video", "mascotLeft", "gate"))
            add(ConnectionRef("mascotLeft", "video", "mascotRight", "image"))
            add(ConnectionRef("mascotLeft", "video", "leftOutput", "file_path"))
            add(ConnectionRef("mascotRight", "video", "rightOutput", "file_path"))
        },
    )
}

/**
 * Just [mascotSubgraph]'s own base generation, no Kontext edits — for a plain character/reference
 * image request (e.g. "T-pose, plain white background, whole body") with no pointing pose needed.
 * Costs one 4-step FLUX schnell call instead of [mascotPreviewWorkflow]'s base + two 20-step
 * Kontext edits.
 */
internal fun characterBaseWorkflow(
    description: String,
    width: Int? = null,
    height: Int? = null,
    useLlmPromptEnhance: Boolean? = null,
): WorkflowDefinition {
    val width = width ?: SHORTS_WIDTH
    val height = height ?: SHORTS_HEIGHT
    val useLlmPromptEnhance = useLlmPromptEnhance ?: false
    return WorkflowDefinition(
        id = "character-base",
        name = "Character Base",
        nodes = listOf(
            NodeRef(
                "base", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotSubgraph(id = "character-base-gen", mascotDescription = description, width = width, height = height, useLlmPromptEnhance = useLlmPromptEnhance),
            ),
            NodeRef("output", "media.file_output"),
        ),
        connections = listOf(ConnectionRef("base", "video", "output", "file_path")),
    )
}
