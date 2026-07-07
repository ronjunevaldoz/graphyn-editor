package com.ronjunevaldoz.graphyn.editor.canvas

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

/**
 * Resolves a node's spec for rendering, falling back to a boundary-derived spec for
 * editor-created subgraph nodes (which have no statically-registered spec), and finally to a
 * minimal placeholder so an unknown node type still renders as an empty titled card.
 * A registered spec always wins.
 */
internal fun resolveRenderSpec(node: NodeRef, nodeSpecs: NodeSpecRegistry): NodeSpec =
    nodeSpecs.resolve(node.type)
        ?: deriveSubgraphSpec(node, nodeSpecs)
        ?: NodeSpec(type = node.type, label = node.type, inputs = emptyList(), outputs = emptyList())

internal data class GraphynPortAnchor(val anchorYDp: Int, val nodeWidthDp: Int)

/**
 * Resolves where a connection endpoint attaches to [node] for the given port. Uses the exact
 * same spec + factory resolution as the port dots in `GraphynNodeLayer`, so bezier endpoints,
 * midpoint handles, and rendered dots can never disagree — subgraph nodes especially, whose spec
 * only exists via [deriveSubgraphSpec].
 */
internal fun resolvePortAnchor(
    node: NodeRef,
    portName: String,
    isInput: Boolean,
    nodeSpecs: NodeSpecRegistry,
    canvasCards: NodeCanvasRegistry?,
): GraphynPortAnchor {
    val spec = resolveRenderSpec(node, nodeSpecs)
    val factory = resolveNodeFactory(node, canvasCards, nodeSpecs)
        ?: FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
    val ports = if (isInput) spec.inputs else spec.outputs
    val index = ports.indexOfFirst { it.name == portName }.coerceAtLeast(0)
    return GraphynPortAnchor(
        anchorYDp = factory.portAnchorY(index, isInput, spec),
        nodeWidthDp = factory.nodeWidth,
    )
}
