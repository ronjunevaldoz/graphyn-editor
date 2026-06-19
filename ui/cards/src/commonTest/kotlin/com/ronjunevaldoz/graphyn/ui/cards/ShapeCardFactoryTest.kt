package com.ronjunevaldoz.graphyn.ui.cards

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShapeCardFactoryTest {

    private val stubSpec = NodeSpec(
        type = "stub", label = "Stub",
        inputs = listOf(PortSpec("in", WorkflowType.OpaqueType)),
        outputs = listOf(PortSpec("out", WorkflowType.OpaqueType)),
    )

    @Test
    fun nodeWidth_matchesSize() {
        val factory = ShapeCardFactory()
        assertEquals(factory.size.value.toInt(), factory.nodeWidth)
    }

    @Test
    fun nodeHeight_includesLabelRow() {
        val factory = ShapeCardFactory()
        assertTrue(factory.nodeHeight > factory.nodeWidth)
    }

    @Test
    fun portAnchorY_centredOnShape() {
        val factory = ShapeCardFactory()
        val expected = factory.size.value.toInt() / 2
        assertEquals(expected, factory.portAnchorY(0, isInput = true, spec = stubSpec))
        assertEquals(expected, factory.portAnchorY(0, isInput = false, spec = stubSpec))
    }
}
