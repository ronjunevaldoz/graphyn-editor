package com.ronjunevaldoz.graphyn.plugins.stylenodes

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// Dark-header card port colors (ComfyUI-inspired palette)
private const val DARK_HEADER_MODEL       = 0xFF6B6BF7L  // blue-purple (model type)
private const val DARK_HEADER_CONDITIONING = 0xFFFF9900L  // orange (conditioning type)
private const val DARK_HEADER_LATENT      = 0xFF9C27B0L  // purple (latent type)

// Field card port colors (Blender-inspired palette)
private const val FIELD_GEOMETRY  = 0xFF3DC95AL  // bright green (geometry)
private const val FIELD_FLOAT     = 0xFFAAAAAAL  // gray (float / value)
private const val FIELD_INT       = 0xFF909090L  // slightly darker gray (int)

object StyleNodesSpecs {
    val kSampler = NodeSpec(
        type = "stylenodes.ksampler",
        label = "KSampler",
        inputs = listOf(
            PortSpec("model",    WorkflowType.OpaqueType, portColor = DARK_HEADER_MODEL),
            PortSpec("positive", WorkflowType.StringType, portColor = DARK_HEADER_CONDITIONING),
            PortSpec("negative", WorkflowType.StringType, portColor = DARK_HEADER_CONDITIONING),
            PortSpec("latent",   WorkflowType.OpaqueType, portColor = DARK_HEADER_LATENT),
        ),
        outputs = listOf(
            PortSpec("latent", WorkflowType.OpaqueType, portColor = DARK_HEADER_LATENT),
        ),
        defaultValues = mapOf(
            "steps" to WorkflowValue.IntValue(20),
            "cfg"   to WorkflowValue.DoubleValue(7.0),
        ),
    )

    val distributePoints = NodeSpec(
        type = "stylenodes.distribute_points",
        label = "Distribute Points",
        inputs = listOf(
            PortSpec("mesh",    WorkflowType.OpaqueType, portColor = FIELD_GEOMETRY),
            PortSpec("density", WorkflowType.DoubleType, portColor = FIELD_FLOAT),
            PortSpec("seed",    WorkflowType.IntType,    portColor = FIELD_INT),
        ),
        outputs = listOf(
            PortSpec("points", WorkflowType.OpaqueType, portColor = FIELD_GEOMETRY),
        ),
        defaultValues = mapOf(
            "density" to WorkflowValue.DoubleValue(0.002),
            "seed"    to WorkflowValue.IntValue(5),
        ),
    )

    val webhook = NodeSpec(
        type = "stylenodes.webhook",
        label = "Webhook",
        inputs = emptyList(),
        outputs = listOf(
            PortSpec("body", WorkflowType.RecordType(emptyMap())),
        ),
    )
}
