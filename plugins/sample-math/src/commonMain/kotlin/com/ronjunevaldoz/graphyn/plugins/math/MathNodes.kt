package com.ronjunevaldoz.graphyn.plugins.math

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private val mathInputs = listOf(
    PortSpec(name = "left", type = WorkflowType.DoubleType, required = false),
    PortSpec(name = "right", type = WorkflowType.DoubleType, required = false),
)
private val mathDefaults = mapOf(
    "left" to WorkflowValue.DoubleValue(0.0),
    "right" to WorkflowValue.DoubleValue(0.0),
)
private val resultOutput = listOf(PortSpec(name = "result", type = WorkflowType.DoubleType))

object MathNodes {
    val add = NodeSpec(type = "math.add", label = "Add", inputs = mathInputs, outputs = resultOutput, defaultValues = mathDefaults)
    val subtract = NodeSpec(type = "math.subtract", label = "Subtract", inputs = mathInputs, outputs = resultOutput, defaultValues = mathDefaults)
    val multiply = NodeSpec(type = "math.multiply", label = "Multiply", inputs = mathInputs, outputs = resultOutput, defaultValues = mathDefaults)
}
