package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CATEGORY_AUTOMATION

private const val DATA = 0xFF64B5F6L
private const val BOOL = 0xFFFFEB3BL

val specSetField = NodeSpec(
    type = "stylenodes.set_field",
    label = "Set Field",
    description = "Adds or overwrites a named field on a data record.",
    category = CATEGORY_AUTOMATION,
    inputs = listOf(
        PortSpec("record", WorkflowType.RecordType(emptyMap()), portColor = DATA, description = "Incoming record"),
        PortSpec("key",    WorkflowType.StringType,             portColor = DATA, description = "Field name"),
        PortSpec("value",  WorkflowType.OpaqueType,             portColor = DATA, description = "Value to assign"),
    ),
    outputs = listOf(
        PortSpec("record", WorkflowType.RecordType(emptyMap()), portColor = DATA, description = "Record with new field"),
    ),
)

val specFilterIf = NodeSpec(
    type = "stylenodes.filter_if",
    label = "Filter If",
    description = "Passes the value through only when the condition is true.",
    category = CATEGORY_AUTOMATION,
    inputs = listOf(
        PortSpec("value",     WorkflowType.OpaqueType,  portColor = DATA, description = "Value to forward"),
        PortSpec("condition", WorkflowType.BooleanType, portColor = BOOL, description = "Gate condition"),
    ),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, portColor = DATA)),
)

val specHttpRequestDemo = NodeSpec(
    type = "stylenodes.http_request",
    label = "HTTP Request",
    description = "Sends an HTTP request to an external endpoint and returns the response body.",
    category = CATEGORY_AUTOMATION,
    inputs = listOf(
        PortSpec("url",    WorkflowType.StringType,                               portColor = DATA, description = "Request URL"),
        PortSpec("method", WorkflowType.EnumType(listOf("GET","POST","PUT","DELETE")),
                                                                                  portColor = DATA, description = "HTTP method"),
        PortSpec("body",   WorkflowType.OpaqueType,                               portColor = DATA, description = "Request body"),
    ),
    outputs = listOf(PortSpec("response", WorkflowType.OpaqueType, portColor = DATA)),
    defaultValues = mapOf("method" to WorkflowValue.StringValue("GET")),
)

val specLogOutput = NodeSpec(
    type = "stylenodes.log_output",
    label = "Log Output",
    description = "Prints the incoming value to the workflow execution log.",
    category = CATEGORY_AUTOMATION,
    inputs = listOf(PortSpec("value", WorkflowType.OpaqueType, portColor = DATA)),
    outputs = emptyList(),
)
