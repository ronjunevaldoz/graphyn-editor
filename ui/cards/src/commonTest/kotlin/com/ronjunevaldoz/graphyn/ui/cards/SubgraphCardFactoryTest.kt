package com.ronjunevaldoz.graphyn.ui.cards

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlin.test.Test
import kotlin.test.assertEquals

private fun subgraphSpec(inputs: Int, outputs: Int): NodeSpec = NodeSpec(
    type = "graphyn.subgraph",
    label = "Subgraph",
    inputs = List(inputs) { i -> PortSpec("in$i", WorkflowType.StringType) },
    outputs = List(outputs) { i -> PortSpec("out$i", WorkflowType.StringType) },
)

class SubgraphCardFactoryTest {
    @Test
    fun nodeHeight_accountsForBoundarySections() {
        val factory = SubgraphCardFactory(inputRows = 2, outputRows = 1)
        val expected = HEADER_DP + SUBGRAPH_SECTION_DP + 2 * ROW_DP + FOOTER_DIVIDER_DP + SUBGRAPH_SECTION_DP + ROW_DP + ENTER_HINT_DP
        assertEquals(expected, factory.nodeHeight)
    }

    @Test
    fun portAnchors_alignWithSectionRows() {
        val factory = SubgraphCardFactory(inputRows = 2, outputRows = 1)
        val spec = subgraphSpec(2, 1)
        assertEquals(HEADER_DP + SUBGRAPH_SECTION_DP + ROW_DP / 2, factory.portAnchorY(0, true, spec))
        assertEquals(HEADER_DP + SUBGRAPH_SECTION_DP + 2 * ROW_DP + FOOTER_DIVIDER_DP + SUBGRAPH_SECTION_DP + ROW_DP / 2, factory.portAnchorY(0, false, spec))
    }
}
