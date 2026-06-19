package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

val groupsDemoWorkflow = WorkflowDefinition(
    id = "groups-demo", name = "Groups",
    nodes = listOf(
        NodeRef("fetch",  "io.http_request"),
        NodeRef("read",   "io.file_read"),
        NodeRef("zip",    "listops.zip"),
        NodeRef("map",    "listops.map"),
        NodeRef("filter", "listops.filter"),
        NodeRef("write",  "io.file_write"),
    ),
    connections = listOf(
        ConnectionRef("fetch",  "body",    "zip",    "listA"),
        ConnectionRef("read",   "content", "zip",    "listB"),
        ConnectionRef("zip",    "result",  "map",    "list"),
        ConnectionRef("map",    "result",  "filter", "list"),
        ConnectionRef("filter", "result",  "write",  "content"),
    ),
)

/** Inner workflow embedded inside the "Transform Pipeline" subgraph node. */
val subgraphInnerWorkflow = WorkflowDefinition(
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

val subgraphDemoWorkflow = WorkflowDefinition(
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
