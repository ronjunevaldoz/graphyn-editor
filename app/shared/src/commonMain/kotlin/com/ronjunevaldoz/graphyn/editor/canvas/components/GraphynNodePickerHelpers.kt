package com.ronjunevaldoz.graphyn.editor.canvas.components

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
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
            val srcIsOpaque = srcPort.type is WorkflowType.OpaqueType
            val out = spec.outputs.firstOrNull { p ->
                WorkflowTypeCompatibility.isCompatible(srcPort.type, p.type)
                    && (srcIsOpaque || p.type !is WorkflowType.OpaqueType)
            } ?: return@mapNotNull null
            spec to out.name
        } else {
            val srcPort = fromSpec.outputs.firstOrNull { it.name == draft.fromPort } ?: return@mapNotNull null
            val srcIsOpaque = srcPort.type is WorkflowType.OpaqueType
            val inp = spec.inputs.firstOrNull { p ->
                WorkflowTypeCompatibility.isCompatible(p.type, srcPort.type)
                    && (srcIsOpaque || p.type !is WorkflowType.OpaqueType)
            } ?: return@mapNotNull null
            spec to inp.name
        }
    }
}
