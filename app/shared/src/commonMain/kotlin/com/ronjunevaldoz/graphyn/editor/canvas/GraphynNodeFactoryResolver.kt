package com.ronjunevaldoz.graphyn.editor.canvas

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.connectedInputPorts
import com.ronjunevaldoz.graphyn.core.model.deriveSubgraphSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

/**
 * Resolves the [NodeCanvasFactory] for [node], in precedence order:
 *  1. a host-registered card for the node type, if any;
 *  2. an editor-created subgraph node → the standard [FieldCardFactory] sized from its derived
 *     boundary ports (double-tap-to-enter is generic on [FieldCardFactory] via
 *     [com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext.onEnterSubgraph]);
 *  3. any other node with a resolvable spec → a default [FieldCardFactory] sized from the spec's
 *     port counts.
 *
 * Returns null only for an unknown node type with no resolvable spec.
 *
 * Centralised so rendering, gesture hit-testing, and viewport fitting all agree on a node's size
 * and behaviour. [workflow] (the workflow the node lives in) keeps connected-but-optional
 * subgraph boundary inputs counted in the card size.
 */
internal fun resolveNodeFactory(
    node: NodeRef,
    canvasCards: NodeCanvasRegistry?,
    nodeSpecs: NodeSpecRegistry?,
    workflow: WorkflowDefinition?,
): NodeCanvasFactory? {
    canvasCards?.resolve(node.type)?.let { return it }
    if (nodeSpecs == null) return null
    if (node.subgraph != null) {
        val connected = workflow?.connectedInputPorts(node.id).orEmpty()
        val derived = deriveSubgraphSpec(node, nodeSpecs, connectedInputs = connected) ?: return null
        return FieldCardFactory(inputRows = derived.inputs.size, outputRows = derived.outputs.size)
    }
    val spec = nodeSpecs.resolve(node.type) ?: return null
    return FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
}
