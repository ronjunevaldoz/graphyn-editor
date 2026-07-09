package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory

/**
 * Canvas factory for collapsed subgraph nodes.
 *
 * The card uses the derived boundary spec for size and anchor math, but renders as a distinct
 * container so the boundary no longer reads like an ordinary editable field list.
 */
class SubgraphCardFactory(
    private val theme: FieldNodeTheme = FieldNodeTheme(),
    private val inputRows: Int,
    private val outputRows: Int,
) : NodeCanvasFactory {
    override val nodeWidth = CARD_WIDTH_DP
    override val nodeHeight =
        HEADER_DP + sectionHeight(inputRows) + dividerHeight(inputRows, outputRows) +
            sectionHeight(outputRows) + ENTER_HINT_DP

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) {
            HEADER_DP + sectionLabelHeight(spec.inputs.size) + portIndex * ROW_DP + ROW_DP / 2
        } else {
            HEADER_DP + sectionLabelHeight(spec.inputs.size) + spec.inputs.size * ROW_DP + dividerHeight(spec.inputs.size, spec.outputs.size) +
                sectionLabelHeight(spec.outputs.size) + portIndex * ROW_DP + ROW_DP / 2
        }

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        CompositionLocalProvider(LocalFieldNodeTheme provides theme) {
            FieldCardColorPickerHost { SubgraphCard(context) }
        }
    }
}

internal const val SUBGRAPH_SECTION_DP = 16

private fun sectionHeight(rows: Int): Int = if (rows == 0) 0 else SUBGRAPH_SECTION_DP + rows * ROW_DP
private fun sectionLabelHeight(rows: Int): Int = if (rows == 0) 0 else SUBGRAPH_SECTION_DP
private fun dividerHeight(inputRows: Int, outputRows: Int): Int =
    if (inputRows > 0 && outputRows > 0) FOOTER_DIVIDER_DP else 0
