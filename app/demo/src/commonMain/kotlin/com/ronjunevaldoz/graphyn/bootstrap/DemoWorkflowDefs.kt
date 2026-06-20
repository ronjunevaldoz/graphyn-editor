package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
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
    id = "subgraph-inner", name = "Transform Pipeline",
    nodes = listOf(
        NodeRef("src",    "io.http_request"),
        NodeRef("zip",    "listops.zip"),
        NodeRef("map",    "listops.map"),
        NodeRef("filter", "listops.filter"),
        NodeRef("reduce", "listops.reduce"),
        NodeRef("sink",   "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("src",    "body",   "zip",    "listA"),
        ConnectionRef("zip",    "result", "map",    "list"),
        ConnectionRef("map",    "result", "filter", "list"),
        ConnectionRef("filter", "result", "reduce", "list"),
        ConnectionRef("reduce", "result", "sink",   "content"),
    ),
)

internal val subgraphDemoWorkflow = WorkflowDefinition(
    id = "subgraph-demo", name = "Subgraph",
    nodes = listOf(
        NodeRef("fetch",    "io.http_request"),
        NodeRef("pipeline", SUBGRAPH_NODE_TYPE, subgraph = subgraphInnerWorkflow),
        NodeRef("write",    "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("fetch",    "body",   "pipeline", "input"),
        ConnectionRef("pipeline", "output", "write",    "content"),
    ),
)
