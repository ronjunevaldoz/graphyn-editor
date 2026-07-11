package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

internal const val COMPARISON_OUTPUT_DIR = "outputs/comparison-short"

internal fun comparisonOutput(fileName: String) = "$COMPARISON_OUTPUT_DIR/$fileName"

/**
 * Comparison-format short: a generated comparison arc drives four paired image scenes, with one
 * recurring mascot pose per pair and narration timing that comes from the measured audio duration.
 */
internal fun comparisonShortWorkflow(
    topic: String,
    width: Int? = null,
    height: Int? = null,
    mascotDescription: String? = null,
    useKenBurns: Boolean? = null,
): WorkflowDefinition {
    val width = width ?: SHORTS_WIDTH
    val height = height ?: SHORTS_HEIGHT
    val mascotDescription = mascotDescription ?: com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION
    val useKenBurns = useKenBurns ?: true
    return WorkflowDefinition(
        id = "comparison-short",
        name = "Comparison Short (X vs Y)",
        nodes = buildList {
            add(comparisonShortGuideNote())
            addAll(comparisonShortNodes(topic, width, height, mascotDescription, useKenBurns))
        },
        connections = comparisonShortConnections(),
    )
}
