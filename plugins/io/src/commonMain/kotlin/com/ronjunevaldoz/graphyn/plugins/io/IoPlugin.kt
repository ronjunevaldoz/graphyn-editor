package com.ronjunevaldoz.graphyn.plugins.io

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val CATEGORY_IO = "graphyn.io"

internal val specHttpRequest = NodeSpec(
    type = "io.http_request",
    label = "HTTP Request",
    description = "Sends an HTTP request to a URL and returns the response body and status code.",
    category = CATEGORY_IO,
    inputs = listOf(
        PortSpec("url", WorkflowType.StringType, description = "Request URL"),
        PortSpec("method", WorkflowType.EnumType(listOf("GET", "POST", "PUT", "DELETE", "PATCH")), description = "HTTP method"),
        PortSpec("body", WorkflowType.NullableType(WorkflowType.StringType), description = "Request body (optional)"),
        PortSpec("headers", WorkflowType.NullableType(WorkflowType.RecordType(emptyMap())), description = "Request headers (optional)"),
    ),
    outputs = listOf(
        PortSpec("body", WorkflowType.StringType, description = "Response body as text"),
        PortSpec("statusCode", WorkflowType.IntType, description = "HTTP status code"),
        PortSpec("ok", WorkflowType.BooleanType, description = "True if status is 2xx"),
    ),
    defaultValues = mapOf(
        "method" to WorkflowValue.StringValue("GET"),
        "url" to WorkflowValue.StringValue("https://"),
    ),
)

internal val specFileRead = NodeSpec(
    type = "io.file_read",
    label = "File Read",
    description = "Reads a file from the local filesystem and emits its text content.",
    category = CATEGORY_IO,
    inputs = listOf(
        PortSpec("path", WorkflowType.StringType, description = "Absolute or relative file path"),
    ),
    outputs = listOf(
        PortSpec("content", WorkflowType.StringType, description = "File contents as text"),
        PortSpec("exists", WorkflowType.BooleanType, description = "True if the file was found"),
    ),
    defaultValues = mapOf("path" to WorkflowValue.StringValue("")),
)

internal val specFileWrite = NodeSpec(
    type = "io.file_write",
    label = "File Write",
    description = "Writes text content to a file on the local filesystem.",
    category = CATEGORY_IO,
    inputs = listOf(
        PortSpec("path", WorkflowType.StringType, description = "Absolute or relative file path"),
        PortSpec("content", WorkflowType.StringType, description = "Text content to write"),
        PortSpec("append", WorkflowType.BooleanType, description = "If true, appends instead of overwriting"),
    ),
    outputs = listOf(
        PortSpec("success", WorkflowType.BooleanType, description = "True if the write succeeded"),
    ),
    defaultValues = mapOf(
        "path" to WorkflowValue.StringValue(""),
        "append" to WorkflowValue.BooleanValue(false),
    ),
)

object IoPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.io",
        displayName = "I/O Operations",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specHttpRequest, specFileRead, specFileWrite).forEach { registrar.registerNodeSpec(it) }
        registrar.registerExecutor(specHttpRequest.type) { inputs ->
            mapOf(
                "body" to WorkflowValue.StringValue(""),
                "statusCode" to WorkflowValue.IntValue(0),
                "ok" to WorkflowValue.BooleanValue(false),
            )
        }
        registrar.registerExecutor(specFileRead.type) { inputs ->
            mapOf(
                "content" to WorkflowValue.StringValue(""),
                "exists" to WorkflowValue.BooleanValue(false),
            )
        }
        registrar.registerExecutor(specFileWrite.type) { inputs ->
            mapOf("success" to WorkflowValue.BooleanValue(false))
        }
    }
}
