package com.ronjunevaldoz.graphyn.plugins.samplelogger

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

object SampleLoggerNodes {
    val log = NodeSpec(
        type = "sample.logger",
        label = "Logger",
        inputs = listOf(
            PortSpec(name = "message", type = WorkflowType.StringType, required = false),
        ),
        outputs = listOf(
            PortSpec(name = "message", type = WorkflowType.StringType, required = false),
        ),
        defaultValues = mapOf(
            "message" to WorkflowValue.StringValue(""),
        ),
    )
}
