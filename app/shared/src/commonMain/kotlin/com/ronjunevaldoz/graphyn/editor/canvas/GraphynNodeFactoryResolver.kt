package com.ronjunevaldoz.graphyn.editor.canvas

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.SubgraphNodeCardFactory

/**
 * Resolves the [NodeCanvasFactory] for [node]: a host-registered card wins; otherwise an
 * editor-created subgraph node falls back to a [SubgraphNodeCardFactory] sized from its derived
 * boundary ports. Returns null for an ordinary node with no registered card (the default card
 * renders it).
 *
 * Centralised so rendering, gesture hit-testing, and viewport fitting all agree on a subgraph
 * node's size and behaviour.
 */
internal fun resolveNodeFactory(
    node: NodeRef,
    canvasCards: NodeCanvasRegistry?,
    nodeSpecs: NodeSpecRegistry?,
): NodeCanvasFactory? {
    canvasCards?.resolve(node.type)?.let { return it }
    if (node.subgraph == null || nodeSpecs == null) return null
    val spec = deriveSubgraphSpec(node, nodeSpecs) ?: return null
    return SubgraphNodeCardFactory(spec.inputs.size, spec.outputs.size)
}
