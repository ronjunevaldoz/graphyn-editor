package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringValue as s
private fun p(x: Int, y: Int) = WorkflowNodePosition(x, y)

internal fun productIngestionPositions() = mapOf(
    "guide" to p(40, 40),
    "fetch" to p(760, 40),
    "parse" to p(1140, 40),
    "normalize" to p(1520, 40),
    "previewRaw" to p(1900, 40),
    "previewNormalized" to p(2280, 40),
    "stringify" to p(2660, 40),
    "write" to p(3040, 40),
)

private const val NORMALIZE_SCRIPT = """
val record = input as Map<*, *>
mapOf(
    "source" to "github",
    "full_name" to (record["full_name"] ?: ""),
    "stars" to (record["stargazers_count"] ?: 0),
    "forks" to (record["forks_count"] ?: 0),
    "language" to (record["language"] ?: "unknown"),
    "updated_at" to (record["updated_at"] ?: ""),
)
"""

internal val productIngestionWorkflow = WorkflowDefinition(
    id = "api-ingestion-product",
    name = "API Ingestion Pro",
    nodePositions = productIngestionPositions(),
    nodes = listOf(
        guideNote("API Ingestion Pro\n\nFetch a public API, normalize the payload into a stable record, validate the schema contract, and persist the result."),
        NodeRef("fetch", "io.http_request", config = mapOf(
            "method" to s("GET"),
            "url" to s("https://api.github.com/repos/JetBrains/kotlin"),
        )),
        NodeRef("parse", "json.parse"),
        NodeRef("normalize", "script.eval", config = mapOf("code" to s(NORMALIZE_SCRIPT))),
        NodeRef("previewRaw", "preview.view"),
        NodeRef("previewNormalized", "preview.view"),
        NodeRef("stringify", "json.stringify", config = mapOf("pretty" to WorkflowValue.BooleanValue(true))),
        NodeRef("write", "io.file_write", config = mapOf("path" to s("api-ingestion-product.json"))),
    ),
    connections = listOf(
        ConnectionRef("fetch", "body", "parse", "text"),
        ConnectionRef("parse", "value", "normalize", "input"),
        ConnectionRef("parse", "value", "previewRaw", "value"),
        ConnectionRef("normalize", "result", "previewNormalized", "value"),
        ConnectionRef("normalize", "result", "stringify", "value"),
        ConnectionRef("stringify", "text", "write", "content"),
    ),
)
