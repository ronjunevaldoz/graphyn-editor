package com.ronjunevaldoz.graphyn.editor.canvas.components

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft

internal fun compatiblePickerSpecs(
    draft: GraphynConnectionDraft,
    workflow: WorkflowDefinition,
    nodeSpecs: NodeSpecRegistry,
): List<Pair<NodeSpec, String>> {
    val fromNode = workflow.nodes.firstOrNull { it.id == draft.fromNodeId } ?: return emptyList()
    val fromSpec = nodeSpecs.resolve(fromNode.type) ?: return emptyList()
    return nodeSpecs.all().mapNotNull { spec ->
        if (draft.isFromInput) {
            val srcPort = fromSpec.inputs.firstOrNull { it.name == draft.fromPort } ?: return@mapNotNull null
            val out = spec.outputs.firstOrNull { p -> PortCompatibility.isCompatible(srcPort, p) } ?: return@mapNotNull null
            spec to out.name
        } else {
            val srcPort = fromSpec.outputs.firstOrNull { it.name == draft.fromPort } ?: return@mapNotNull null
            val inp = spec.inputs.firstOrNull { p -> PortCompatibility.isCompatible(p, srcPort) } ?: return@mapNotNull null
            spec to inp.name
        }
    }
}
