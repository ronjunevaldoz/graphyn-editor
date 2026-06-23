package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CATEGORY_GEOMETRY

private const val GEO   = 0xFF3DC95AL
private const val FLOAT = 0xFFAAAAAAL
private const val INT   = 0xFF909090L

val specMeshPrimitive = NodeSpec(
    type = "stylenodes.mesh_primitive",
    label = "Mesh Primitive",
    description = "Generates a parametric mesh primitive: cube, sphere, cylinder, or plane.",
    category = CATEGORY_GEOMETRY,
    inputs = listOf(
        PortSpec("size",         WorkflowType.DoubleType,                                       portColor = FLOAT, description = "Uniform scale"),
        PortSpec("subdivisions", WorkflowType.IntType,                                          portColor = INT,   description = "Subdivision level"),
        PortSpec("type",         WorkflowType.EnumType(listOf("cube","sphere","cylinder","plane")),
                                                                                                portColor = INT,   description = "Primitive shape"),
    ),
    outputs = listOf(PortSpec("geometry", WorkflowType.OpaqueType, portColor = GEO)),
    defaultValues = mapOf(
        "size" to WorkflowValue.DoubleValue(1.0),
        "subdivisions" to WorkflowValue.IntValue(0),
        "type" to WorkflowValue.StringValue("cube"),
    ),
)

val specSubdivideMesh = NodeSpec(
    type = "stylenodes.subdivide_mesh",
    label = "Subdivide Mesh",
    description = "Increases mesh resolution by subdividing each face.",
    category = CATEGORY_GEOMETRY,
    inputs = listOf(
        PortSpec("geometry", WorkflowType.OpaqueType, portColor = GEO,   description = "Input mesh"),
        PortSpec("level",    WorkflowType.IntType,    portColor = INT,   description = "Subdivision iterations"),
    ),
    outputs = listOf(PortSpec("geometry", WorkflowType.OpaqueType, portColor = GEO)),
    defaultValues = mapOf("level" to WorkflowValue.IntValue(2)),
)

val specInstanceOnPoints = NodeSpec(
    type = "stylenodes.instance_on_points",
    label = "Instance on Points",
    description = "Places instances of a geometry at each point in a point cloud.",
    category = CATEGORY_GEOMETRY,
    inputs = listOf(
        PortSpec("points",   WorkflowType.OpaqueType, portColor = GEO, description = "Point cloud"),
        PortSpec("instance", WorkflowType.OpaqueType, portColor = GEO, description = "Geometry to instantiate"),
    ),
    outputs = listOf(PortSpec("geometry", WorkflowType.OpaqueType, portColor = GEO)),
)

val specGeometryOutput = NodeSpec(
    type = "stylenodes.geometry_output",
    label = "Geometry Output",
    description = "Marks the final geometry result of the node tree.",
    category = CATEGORY_GEOMETRY,
    inputs = listOf(PortSpec("geometry", WorkflowType.OpaqueType, portColor = GEO)),
    outputs = emptyList(),
)
