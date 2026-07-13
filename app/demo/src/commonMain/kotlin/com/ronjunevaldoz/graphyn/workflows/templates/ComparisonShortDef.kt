package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

internal const val COMPARISON_OUTPUT_DIR = "outputs/comparison-short"

internal fun comparisonOutput(fileName: String) = "$COMPARISON_OUTPUT_DIR/$fileName"

/**
 * Comparison-format short: a generated comparison arc drives four paired image scenes, with one
 * recurring mascot pose per pair and narration timing that comes from the measured audio duration.
 *
 * The mascot is supplied as two ready-made images ([mascotLeftImagePath]/[mascotRightImagePath] —
 * left-pointing and right-pointing), not generated in-workflow. Generating + Kontext-editing a
 * consistent mascot was the single most fragile part of this pipeline (see MascotScene.kt's doc
 * comments); bringing your own images from any source (local SD, ChatGPT, Gemini, manual art)
 * sidesteps that entirely. [mascotSubgraph]/[mascotPointEditSubgraph] still exist for the
 * `mascot-preview` CLI workflow if you want to generate a mascot to use here.
 */
internal fun comparisonShortWorkflow(
    topic: String,
    width: Int? = null,
    height: Int? = null,
    mascotLeftImagePath: String = "",
    mascotRightImagePath: String = "",
    useKenBurns: Boolean? = null,
): WorkflowDefinition {
    val width = width ?: SHORTS_WIDTH
    val height = height ?: SHORTS_HEIGHT
    val useKenBurns = useKenBurns ?: true
    return WorkflowDefinition(
        id = "comparison-short",
        name = "Comparison Short (X vs Y)",
        nodes = buildList {
            add(comparisonShortGuideNote())
            addAll(comparisonShortNodes(topic, width, height, mascotLeftImagePath, mascotRightImagePath, useKenBurns))
        },
        connections = comparisonShortConnections(),
    )
}
