package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesSpecs

internal val styleNodesDemoWorkflow = WorkflowDefinition(
    id = "style-demo", name = "Style Demo",
    nodes = listOf(
        NodeRef("webhook", StyleNodesSpecs.webhook.type),
        NodeRef("sampler", StyleNodesSpecs.kSampler.type),
        NodeRef("scatter", StyleNodesSpecs.distributePoints.type),
    ),
    connections = listOf(ConnectionRef("sampler", "latent", "scatter", "mesh")),
)

internal val listOpsDemoWorkflow = WorkflowDefinition(
    id = "list-ops-demo", name = "List Ops",
    nodes = listOf(
        NodeRef("zip",    "listops.zip"),
        NodeRef("map",    "listops.map"),
        NodeRef("filter", "listops.filter"),
        NodeRef("reduce", "listops.reduce"),
    ),
    connections = listOf(
        ConnectionRef("zip",    "result", "map",    "list"),
        ConnectionRef("map",    "result", "filter", "list"),
        ConnectionRef("filter", "result", "reduce", "list"),
    ),
)

internal val controlDemoWorkflow = WorkflowDefinition(
    id = "control-demo", name = "Control Flow",
    nodes = listOf(
        NodeRef("loop",   "control.loop"),
        NodeRef("branch", "control.branch"),
        NodeRef("merge",  "control.merge"),
    ),
    connections = listOf(
        ConnectionRef("loop",   "item",      "branch", "value"),
        ConnectionRef("branch", "truePath",  "merge",  "a"),
        ConnectionRef("branch", "falsePath", "merge",  "b"),
    ),
)

internal val textDemoWorkflow = WorkflowDefinition(
    id = "text-demo", name = "Text Ops",
    nodes = listOf(
        NodeRef("format", "text.format"),
        NodeRef("split",  "text.split"),
        NodeRef("regex",  "text.regex"),
    ),
    connections = listOf(ConnectionRef("format", "result", "split", "text")),
)

internal val typesDemoWorkflow = WorkflowDefinition(
    id = "types-demo", name = "Type Utils",
    nodes = listOf(
        NodeRef("schema",   "types.schema"),
        NodeRef("cast",     "types.cast"),
        NodeRef("validate", "types.validate"),
    ),
    connections = listOf(
        ConnectionRef("schema", "schema", "validate", "schema"),
        ConnectionRef("cast",   "result", "validate", "value"),
    ),
)

internal val ioDemoWorkflow = WorkflowDefinition(
    id = "io-demo", name = "I/O",
    nodes = listOf(
        NodeRef("request", "io.http_request"),
        NodeRef("read",    "io.file_read"),
        NodeRef("write",   "io.file_write"),
    ),
    connections = listOf(ConnectionRef("request", "body", "write", "content")),
)

private val subgraphInnerWorkflow = WorkflowDefinition(
    id = "subgraph-inner", name = "File Copy Pipeline",
    nodes = listOf(
        NodeRef("browse", "io.file_browse"),
        NodeRef("read",   "io.file_read"),
        NodeRef("sink",   "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("browse", "path",    "read",  "path"),
        ConnectionRef("read",   "content", "sink",  "content"),
    ),
)

internal val subgraphDemoWorkflow = WorkflowDefinition(
    id = "subgraph-demo", name = "Subgraph",
    nodes = listOf(
        NodeRef("src",      "io.file_browse"),
        NodeRef("pipeline", SUBGRAPH_NODE_TYPE, subgraph = subgraphInnerWorkflow),
        NodeRef("out_dir",  "io.folder_browse"),
        NodeRef("write",    "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("src",      "path",   "pipeline", "input"),
        ConnectionRef("out_dir",  "path",   "write",    "path"),
        ConnectionRef("pipeline", "output", "write",    "content"),
    ),
)

/**
 * Production-shaped API ingestion pipeline: fetch a JSON document over HTTP, parse it,
 * extract fields by path, re-serialize, and persist to a file.
 *
 * Demonstrates the canonical fetch → parse → transform → persist flow and, with the
 * resilient executor, live per-node status plus partial results when a node fails
 * (e.g. no network: `fetch` errors, downstream nodes are skipped, independent ones still run).
 */
internal val apiIngestionDemoWorkflow = WorkflowDefinition(
    id = "api-ingestion-demo", name = "API Ingestion",
    nodes = listOf(
        NodeRef("fetch", "io.http_request", config = mapOf(
            "url" to WorkflowValue.StringValue("https://api.github.com/repos/JetBrains/kotlin"),
            "method" to WorkflowValue.StringValue("GET"),
        )),
        NodeRef("parse", "json.parse"),
        NodeRef("stars", "json.path", config = mapOf("path" to WorkflowValue.StringValue("stargazers_count"))),
        NodeRef("forks", "json.path", config = mapOf("path" to WorkflowValue.StringValue("forks_count"))),
        NodeRef("pretty", "json.stringify", config = mapOf("pretty" to WorkflowValue.BooleanValue(true))),
        NodeRef("save", "io.file_write", config = mapOf("path" to WorkflowValue.StringValue("repo.json"))),
    ),
    connections = listOf(
        ConnectionRef("fetch",  "body",  "parse",  "text"),
        ConnectionRef("parse",  "value", "stars",  "value"),
        ConnectionRef("parse",  "value", "forks",  "value"),
        ConnectionRef("parse",  "value", "pretty", "value"),
        ConnectionRef("pretty", "text",  "save",   "content"),
    ),
)
