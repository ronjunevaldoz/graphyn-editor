package com.ronjunevaldoz.graphyn.editor.canvas

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.SubgraphNodeCardFactory
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

/**
 * Resolves the [NodeCanvasFactory] for [node], in precedence order:
 *  1. a host-registered card for the node type, if any;
 *  2. an editor-created subgraph node → a [SubgraphNodeCardFactory] sized from its derived
 *     boundary ports;
 *  3. any other node with a resolvable spec → a default [FieldCardFactory] sized from the spec's
 *     port counts.
 *
 * Returns null only for an unknown node type with no resolvable spec.
 *
 * Centralised so rendering, gesture hit-testing, and viewport fitting all agree on a node's size
 * and behaviour.
 */
internal fun resolveNodeFactory(
    node: NodeRef,
    canvasCards: NodeCanvasRegistry?,
    nodeSpecs: NodeSpecRegistry?,
): NodeCanvasFactory? {
    canvasCards?.resolve(node.type)?.let { return it }
    if (nodeSpecs == null) return null
    if (node.subgraph != null) {
        val derived = deriveSubgraphSpec(node, nodeSpecs) ?: return null
        return SubgraphNodeCardFactory(derived.inputs.size, derived.outputs.size)
    }
    val spec = nodeSpecs.resolve(node.type) ?: return null
    return FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
}
