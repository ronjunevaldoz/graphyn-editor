package com.ronjunevaldoz.graphyn.plugins.stylenodes

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

object StyleNodesSpecs {
    val comfyKSampler = NodeSpec(
        type = "comfy.ksampler",
        label = "KSampler",
        inputs = listOf(
            PortSpec("model",    WorkflowType.OpaqueType),
            PortSpec("positive", WorkflowType.StringType),
            PortSpec("negative", WorkflowType.StringType),
            PortSpec("latent",   WorkflowType.OpaqueType),
        ),
        outputs = listOf(
            PortSpec("latent", WorkflowType.OpaqueType),
        ),
        defaultValues = mapOf(
            "steps" to WorkflowValue.IntValue(20),
            "cfg"   to WorkflowValue.DoubleValue(7.0),
        ),
    )

    val blenderDistribute = NodeSpec(
        type = "blender.distribute_points",
        label = "Distribute Points",
        inputs = listOf(
            PortSpec("mesh",    WorkflowType.OpaqueType),
            PortSpec("density", WorkflowType.DoubleType),
            PortSpec("seed",    WorkflowType.IntType),
        ),
        outputs = listOf(
            PortSpec("points", WorkflowType.OpaqueType),
        ),
        defaultValues = mapOf(
            "density" to WorkflowValue.DoubleValue(0.002),
            "seed"    to WorkflowValue.IntValue(5),
        ),
    )

    val n8nWebhook = NodeSpec(
        type = "n8n.webhook",
        label = "Webhook",
        inputs = emptyList(),
        outputs = listOf(
            PortSpec("body", WorkflowType.RecordType(emptyMap())),
        ),
    )
}
