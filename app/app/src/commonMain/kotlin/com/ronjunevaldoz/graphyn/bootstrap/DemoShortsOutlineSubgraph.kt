package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal fun shortsOutlineSubgraph() = WorkflowDefinition(
    id = "shorts-outline",
    name = "Shorts Outline",
    nodes = listOf(
        NodeRef("ollama_host", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_HOST"))),
        NodeRef("ollama_model", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_MODEL"))),
        NodeRef("ollama_url", "script.eval", config = mapOf("code" to WorkflowValue.StringValue(
            """val host = (input as? String)?.ifBlank { null } ?: "http://localhost:11434"
if (host.endsWith("/")) host.dropLast(1) + "/api/generate" else host + "/api/generate"""
        ))),
        NodeRef("ollama_body", "script.eval", config = mapOf("code" to WorkflowValue.StringValue(SHORTS_BODY_SCRIPT))),
        NodeRef("body_json", "json.stringify"),
        NodeRef("request", "io.http_request", config = mapOf(
            "method" to WorkflowValue.StringValue("POST"),
            "headers" to WorkflowValue.RecordValue(mapOf("Content-Type" to WorkflowValue.StringValue("application/json"))),
        )),
        NodeRef("outer", "json.parse"),
        NodeRef("response", "json.path", config = mapOf("path" to WorkflowValue.StringValue("response"))),
    ),
    connections = listOf(
        ConnectionRef("ollama_host", "value", "ollama_url", "input"),
        ConnectionRef("ollama_model", "value", "ollama_body", "input"),
        ConnectionRef("ollama_url", "result", "request", "url"),
        ConnectionRef("body_json", "text", "request", "body"),
        ConnectionRef("ollama_body", "result", "body_json", "value"),
        ConnectionRef("request", "body", "outer", "text"),
        ConnectionRef("outer", "value", "response", "value"),
    ),
)
