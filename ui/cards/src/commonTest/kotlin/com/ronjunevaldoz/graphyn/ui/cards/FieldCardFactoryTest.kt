package com.ronjunevaldoz.graphyn.ui.cards

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun stubSpec(inputs: Int, outputs: Int): NodeSpec = NodeSpec(
    type = "stub",
    label = "Stub",
    inputs = List(inputs) { i -> PortSpec("in$i", WorkflowType.StringType) },
    outputs = List(outputs) { i -> PortSpec("out$i", WorkflowType.StringType) },
)

class FieldCardFactoryTest {

    @Test
    fun nodeHeight_matchesFormula() {
        val factory = FieldCardFactory(inputRows = 2, outputRows = 1)
        val expected = HEADER_DP + 2 * ROW_DP + FOOTER_DIVIDER_DP + 1 * ROW_DP
        assertEquals(expected, factory.nodeHeight)
    }

    @Test
    fun nodeWidth_isFixed() {
        assertEquals(260, FieldCardFactory().nodeWidth)
    }

    @Test
    fun portAnchorY_inputCentredInRow() {
        val factory = FieldCardFactory(inputRows = 3, outputRows = 2)
        val anchor = factory.portAnchorY(portIndex = 1, isInput = true, spec = stubSpec(inputs = 3, outputs = 2))
        assertEquals(HEADER_DP + 1 * ROW_DP + ROW_DP / 2, anchor)
    }

    @Test
    fun portAnchorY_outputBelowDivider() {
        val factory = FieldCardFactory(inputRows = 2, outputRows = 2)
        val anchor = factory.portAnchorY(portIndex = 0, isInput = false, spec = stubSpec(inputs = 2, outputs = 2))
        assertEquals(HEADER_DP + 2 * ROW_DP + FOOTER_DIVIDER_DP + ROW_DP / 2, anchor)
    }

    @Test
    fun defaultInputAndOutputRows_arePositive() {
        val factory = FieldCardFactory()
        assertTrue(factory.inputRows > 0)
        assertTrue(factory.outputRows > 0)
    }
}
