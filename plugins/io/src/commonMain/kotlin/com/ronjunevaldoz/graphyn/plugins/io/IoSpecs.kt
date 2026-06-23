package com.ronjunevaldoz.graphyn.plugins.io

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

const val CATEGORY_IO = "graphyn.io"

internal val specHttpRequest = NodeSpec(
    type = "io.http_request",
    label = "HTTP Request",
    description = "Sends an HTTP request to a URL and returns the response body and status code.",
    category = CATEGORY_IO,
    inputs = listOf(
        PortSpec("url", WorkflowType.StringType, description = "Request URL"),
        PortSpec("method", WorkflowType.EnumType(listOf("GET", "POST", "PUT", "DELETE", "PATCH")), description = "HTTP method"),
        PortSpec("body", WorkflowType.NullableType(WorkflowType.StringType), required = false, description = "Request body (optional)"),
        PortSpec("headers", WorkflowType.NullableType(WorkflowType.RecordType(emptyMap())), required = false, description = "Request headers (optional)"),
    ),
    outputs = listOf(
        PortSpec("body", WorkflowType.StringType, description = "Response body as text"),
        PortSpec("statusCode", WorkflowType.IntType, description = "HTTP status code"),
        PortSpec("ok", WorkflowType.BooleanType, description = "True if status is 2xx"),
    ),
    defaultValues = mapOf("method" to WorkflowValue.StringValue("GET"), "url" to WorkflowValue.StringValue("https://")),
)

internal val specFileRead = NodeSpec(
    type = "io.file_read", label = "File Read", category = CATEGORY_IO,
    description = "Reads a file from the local filesystem and emits its text content.",
    inputs = listOf(PortSpec("path", WorkflowType.StringType, description = "Absolute or relative file path")),
    outputs = listOf(
        PortSpec("content", WorkflowType.StringType, description = "File contents as text"),
        PortSpec("exists", WorkflowType.BooleanType, description = "True if the file was found"),
    ),
    defaultValues = mapOf("path" to WorkflowValue.StringValue("")),
)

internal val specFileWrite = NodeSpec(
    type = "io.file_write", label = "File Write", category = CATEGORY_IO,
    description = "Writes text content to a file on the local filesystem.",
    inputs = listOf(
        PortSpec("path", WorkflowType.StringType, description = "Absolute or relative file path"),
        PortSpec("content", WorkflowType.StringType, description = "Text content to write"),
        PortSpec("append", WorkflowType.BooleanType, description = "If true, appends instead of overwriting"),
    ),
    outputs = listOf(PortSpec("success", WorkflowType.BooleanType, description = "True if the write succeeded")),
    defaultValues = mapOf("path" to WorkflowValue.StringValue(""), "append" to WorkflowValue.BooleanValue(false)),
)

internal val specFileBrowse = NodeSpec(
    type = "io.file_browse", label = "File Browser", category = CATEGORY_IO,
    inputs = emptyList(),
    outputs = listOf(PortSpec("path", WorkflowType.StringType, description = "Absolute path of the selected file")),
    defaultValues = mapOf("path" to WorkflowValue.StringValue("")),
)

internal val specFolderBrowse = NodeSpec(
    type = "io.folder_browse", label = "Folder Browser", category = CATEGORY_IO,
    inputs = emptyList(),
    outputs = listOf(PortSpec("path", WorkflowType.StringType, description = "Absolute path of the selected folder")),
    defaultValues = mapOf("path" to WorkflowValue.StringValue("")),
)

internal val specWebhookPost = NodeSpec(
    type = "net.webhook_post", label = "Webhook POST", category = CATEGORY_IO,
    description = "Sends a POST request with a JSON payload to a webhook URL.",
    inputs = listOf(
        PortSpec("url", WorkflowType.StringType, description = "Webhook endpoint URL"),
        PortSpec("payload", WorkflowType.StringType, description = "JSON payload string"),
    ),
    outputs = listOf(
        PortSpec("ok", WorkflowType.BooleanType, description = "True if the server responded with 2xx"),
        PortSpec("statusCode", WorkflowType.IntType, description = "HTTP status code"),
    ),
    defaultValues = mapOf("url" to WorkflowValue.StringValue("https://"), "payload" to WorkflowValue.StringValue("{}")),
)

internal val specEnvRead = NodeSpec(
    type = "env.read", label = "Env Variable", category = CATEGORY_IO,
    description = "Reads a process environment variable by name.",
    inputs = listOf(PortSpec("name", WorkflowType.StringType, description = "Environment variable name")),
    outputs = listOf(
        PortSpec("value", WorkflowType.NullableType(WorkflowType.StringType), description = "Value, or null if not set"),
        PortSpec("found", WorkflowType.BooleanType, description = "True if the variable is set"),
    ),
    defaultValues = mapOf("name" to WorkflowValue.StringValue("")),
)
