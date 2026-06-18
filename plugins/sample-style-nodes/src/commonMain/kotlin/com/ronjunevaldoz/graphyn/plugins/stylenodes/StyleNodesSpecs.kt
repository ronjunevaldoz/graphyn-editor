package com.ronjunevaldoz.graphyn.plugins.stylenodes

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Port and category colours — internal so StyleNodesEditorPlugin can reference them
internal const val COLOR_MODEL        = 0xFF6B6BF7L
internal const val COLOR_CONDITIONING = 0xFFFF9900L
internal const val COLOR_LATENT       = 0xFF9C27B0L
internal const val COLOR_GEOMETRY     = 0xFF3DC95AL
private const val COLOR_FLOAT         = 0xFFAAAAAAL
private const val COLOR_INT           = 0xFF909090L

const val CATEGORY_AI = "stylenodes.ai"
const val CATEGORY_GEOMETRY = "stylenodes.geometry"
const val CATEGORY_AUTOMATION = "stylenodes.automation"

object StyleNodesSpecs {
    val kSampler = NodeSpec(
        type = "stylenodes.ksampler",
        label = "KSampler",
        description = "Runs the diffusion sampling loop using a model, conditioning, and latent image.",
        category = CATEGORY_AI,
        inputs = listOf(
            PortSpec("model",    WorkflowType.OpaqueType, portColor = COLOR_MODEL),
            PortSpec("positive", WorkflowType.StringType, portColor = COLOR_CONDITIONING),
            PortSpec("negative", WorkflowType.StringType, portColor = COLOR_CONDITIONING),
            PortSpec("latent",   WorkflowType.OpaqueType, portColor = COLOR_LATENT),
        ),
        outputs = listOf(
            PortSpec("latent", WorkflowType.OpaqueType, portColor = COLOR_LATENT),
        ),
        defaultValues = mapOf(
            "steps" to WorkflowValue.IntValue(20),
            "cfg"   to WorkflowValue.DoubleValue(7.0),
        ),
    )

    val distributePoints = NodeSpec(
        type = "stylenodes.distribute_points",
        label = "Distribute Points",
        description = "Scatters points across a mesh surface using the chosen density and distribution mode.",
        category = CATEGORY_GEOMETRY,
        inputs = listOf(
            PortSpec("mesh",     WorkflowType.OpaqueType,                                          portColor = COLOR_GEOMETRY),
            PortSpec("density",  WorkflowType.DoubleType,                                          portColor = COLOR_FLOAT),
            PortSpec("seed",     WorkflowType.IntType,                                             portColor = COLOR_INT),
            PortSpec("mode",     WorkflowType.EnumType(listOf("uniform", "random", "poisson")),    portColor = COLOR_INT),
            PortSpec("channels", WorkflowType.MultiEnumType(listOf("R", "G", "B", "A")),           portColor = COLOR_INT),
        ),
        outputs = listOf(
            PortSpec("points", WorkflowType.OpaqueType, portColor = COLOR_GEOMETRY),
        ),
        defaultValues = mapOf(
            "density"  to WorkflowValue.DoubleValue(0.002),
            "seed"     to WorkflowValue.IntValue(5),
            "mode"     to WorkflowValue.StringValue("uniform"),
            "channels" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue("R"), WorkflowValue.StringValue("G"), WorkflowValue.StringValue("B"))),
        ),
    )

    val webhook = NodeSpec(
        type = "stylenodes.webhook",
        label = "Webhook",
        description = "Receives an incoming HTTP request and emits its body as a record into the workflow.",
        category = CATEGORY_AUTOMATION,
        inputs = emptyList(),
        outputs = listOf(
            PortSpec("body", WorkflowType.RecordType(emptyMap())),
        ),
    )
}
