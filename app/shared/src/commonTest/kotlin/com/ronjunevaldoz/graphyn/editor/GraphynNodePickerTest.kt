package com.ronjunevaldoz.graphyn.editor

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.compatiblePickerSpecs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphynNodePickerTest {

    private val stringSpec = NodeSpec(
        type = "text.upper",
        label = "Upper",
        inputs = listOf(PortSpec("text", WorkflowType.StringType)),
        outputs = listOf(PortSpec("result", WorkflowType.StringType)),
    )
    private val opaqueSpec = NodeSpec(
        type = "demo.opaque",
        label = "Opaque",
        inputs = listOf(PortSpec("input", WorkflowType.OpaqueType)),
        outputs = listOf(PortSpec("output", WorkflowType.OpaqueType)),
    )

    private val registry = DefaultNodeSpecRegistry().apply {
        register(stringSpec)
        register(opaqueSpec)
    }

    private val workflow = WorkflowDefinition(
        id = "w",
        name = "W",
        nodes = listOf(
            NodeRef("src-str", "text.upper"),
            NodeRef("src-opq", "demo.opaque"),
        ),
        connections = emptyList(),
    )

    @Test
    fun typedOutputExcludesOpaqueInputNode() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-str", fromPort = "result", isFromInput = false)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.first.type }
        assertTrue("text.upper" in types, "StringType output should match StringType input")
        assertFalse("demo.opaque" in types, "StringType output must not match OpaqueType input")
    }

    @Test
    fun opaqueOutputMatchesAnyInputNode() {
        // OpaqueType is a bidirectional wildcard: an opaque output may feed any typed input.
        val draft = GraphynConnectionDraft(fromNodeId = "src-opq", fromPort = "output", isFromInput = false)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.first.type }
        assertTrue("demo.opaque" in types, "OpaqueType output should match OpaqueType input")
        assertTrue("text.upper" in types, "OpaqueType output is a wildcard and should match StringType input")
    }

    @Test
    fun typedInputExcludesOpaqueOutputNode() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-str", fromPort = "text", isFromInput = true)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.first.type }
        assertTrue("text.upper" in types, "StringType input should match StringType output")
        assertFalse("demo.opaque" in types, "StringType input must not be fed by OpaqueType output")
    }
}
