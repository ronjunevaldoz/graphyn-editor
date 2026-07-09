package com.ronjunevaldoz.graphyn.editor

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.canvasPortColor
import com.ronjunevaldoz.graphyn.editor.canvas.components.portColor
import com.ronjunevaldoz.graphyn.editor.canvas.components.compatiblePickerSpecs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphynNodePickerTest {

    private val COLOR_A = 0xFF_AA0000L
    private val COLOR_B = 0xFF_0000AAL

    private val stringSpec = NodeSpec(
        type = "text.upper",
        label = "Upper",
        inputs = listOf(PortSpec("text", WorkflowType.StringType)),
        outputs = listOf(PortSpec("result", WorkflowType.StringType)),
    )
    private val stringColorA = NodeSpec(
        type = "text.color-a",
        label = "String A",
        inputs = listOf(PortSpec("input", WorkflowType.StringType, portColor = COLOR_A)),
        outputs = listOf(PortSpec("output", WorkflowType.StringType, portColor = COLOR_A)),
    )
    private val stringColorB = NodeSpec(
        type = "text.color-b",
        label = "String B",
        inputs = listOf(PortSpec("input", WorkflowType.StringType, portColor = COLOR_B)),
        outputs = listOf(PortSpec("output", WorkflowType.StringType, portColor = COLOR_B)),
    )
    // Generic opaque (no portColor) — like Branch/Map/Filter
    private val opaqueSpec = NodeSpec(
        type = "demo.opaque",
        label = "Opaque",
        inputs = listOf(PortSpec("input", WorkflowType.OpaqueType)),
        outputs = listOf(PortSpec("output", WorkflowType.OpaqueType)),
    )
    // Colored opaque channel A — like sd.txt2img accepting COLOR_MODEL context
    private val opaqueColorA = NodeSpec(
        type = "demo.color-a",
        label = "Color A",
        inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, portColor = COLOR_A)),
        outputs = listOf(PortSpec("output", WorkflowType.OpaqueType, portColor = COLOR_A)),
    )
    // Colored opaque channel B — different semantic channel
    private val opaqueColorB = NodeSpec(
        type = "demo.color-b",
        label = "Color B",
        inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, portColor = COLOR_B)),
        outputs = listOf(PortSpec("output", WorkflowType.OpaqueType, portColor = COLOR_B)),
    )

    private val registry = DefaultNodeSpecRegistry().apply {
        register(stringSpec)
        register(stringColorA)
        register(stringColorB)
        register(opaqueSpec)
        register(opaqueColorA)
        register(opaqueColorB)
    }

    private val workflow = WorkflowDefinition(
        id = "w",
        name = "W",
        nodes = listOf(
            NodeRef("src-str",  "text.upper"),
            NodeRef("src-sa",   "text.color-a"),
            NodeRef("src-sb",   "text.color-b"),
            NodeRef("src-opq",  "demo.opaque"),
            NodeRef("src-ca",   "demo.color-a"),
            NodeRef("src-cb",   "demo.color-b"),
        ),
        connections = emptyList(),
    )

    @Test
    fun typedOutputExcludesOpaqueInputNode() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-str", fromPort = "result", isFromInput = false)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.spec.type }
        assertTrue("text.upper" in types, "StringType output should match StringType input")
        assertFalse("demo.opaque" in types, "StringType output must not match OpaqueType input")
        assertFalse("demo.color-a" in types, "StringType output must not match colored OpaqueType input")
    }

    @Test
    fun opaqueOutputOnlyMatchesSameColorOpaque() {
        // Uncolored (null) opaque output matches only uncolored opaque inputs.
        val draft = GraphynConnectionDraft(fromNodeId = "src-opq", fromPort = "output", isFromInput = false)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.spec.type }
        assertTrue("demo.opaque" in types, "null-color OpaqueType should match null-color OpaqueType input")
        assertFalse("text.upper" in types, "OpaqueType output must not match StringType input")
        assertFalse("demo.color-a" in types, "null-color OpaqueType must not match COLOR_A input")
        assertFalse("demo.color-b" in types, "null-color OpaqueType must not match COLOR_B input")
    }

    @Test
    fun coloredOpaqueOutputOnlyMatchesSameColor() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-ca", fromPort = "output", isFromInput = false)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.spec.type }
        assertTrue("demo.color-a" in types, "COLOR_A output should match COLOR_A input")
        assertFalse("demo.opaque" in types, "COLOR_A output must not match null-color input")
        assertFalse("demo.color-b" in types, "COLOR_A output must not match COLOR_B input")
        assertFalse("text.upper" in types, "Colored OpaqueType output must not match StringType input")
    }

    @Test
    fun typedInputExcludesOpaqueOutputNode() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-str", fromPort = "text", isFromInput = true)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.spec.type }
        assertTrue("text.upper" in types, "StringType input should match StringType output")
        assertFalse("demo.opaque" in types, "StringType input must not be fed by OpaqueType output")
        assertFalse("demo.color-a" in types, "StringType input must not be fed by colored OpaqueType output")
    }

    @Test
    fun pickerSuggestionAccentMatchesTheCompatiblePortColor() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-ca", fromPort = "output", isFromInput = false)
        val suggestion = compatiblePickerSpecs(draft, workflow, registry).first { it.spec.type == "demo.color-a" }
        assertEquals(opaqueColorA.inputs.first().canvasPortColor("demo.color-a"), suggestion.accentColor)
    }

    @Test
    fun pickerSuggestionsAreFilteredByColor() {
        val draft = GraphynConnectionDraft(fromNodeId = "src-sa", fromPort = "output", isFromInput = false)
        val types = compatiblePickerSpecs(draft, workflow, registry).map { it.spec.type }
        assertTrue("text.color-a" in types, "Matching color should stay visible")
        assertFalse("text.color-b" in types, "Mismatched color should be filtered out")
        assertFalse("text.upper" in types, "Default string color should be filtered out when source color is explicit")
    }
}
