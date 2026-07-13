package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * The Ollama request -> outer json.parse -> response json.path -> inner json.parse chain shared by
 * [storyboardGeneratorSubgraph] and [comparisonGeneratorSubgraph]. [bodyNodeType] selects which
 * ollama_body builder to use ([ShortsNodeTypes.OLLAMA_BODY] or [ShortsNodeTypes.COMPARISON_OLLAMA_BODY])
 * since the two prompts request different JSON schemas; everything else — URL building, the HTTP
 * call, both parse steps, and the chain-diagnostics bundling — is identical.
 *
 * Exposes exactly two free outputs: the parsed `value` and a bundled `diagnostics` summary (built by
 * the internal [ShortsNodeTypes.OLLAMA_CHAIN_DIAGNOSTICS] node from every stage's ok/error signal).
 * A caller's validate node wires just those two ports instead of one connection per pipeline stage —
 * this replaced a design where all 4 internal stages wired directly into validate, which read as an
 * unreadable fan-in of ~9 wires converging on one node for what is fundamentally diagnostics, not
 * business data.
 */
public fun ollamaFetchSubgraph(
    id: String,
    bodyNodeType: String,
    topic: String,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Ollama Fetch",
    nodes = listOf(
        NodeRef("ollama_host", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_HOST"))),
        NodeRef("ollama_model", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_MODEL"))),
        NodeRef("ollama_url", ShortsNodeTypes.OLLAMA_URL),
        NodeRef("ollama_body", bodyNodeType, config = mapOf("topic" to WorkflowValue.StringValue(topic))),
        NodeRef("body_json", "json.stringify"),
        NodeRef(
            "request",
            "io.http_request",
            config = mapOf(
                "method" to WorkflowValue.StringValue("POST"),
                "headers" to WorkflowValue.RecordValue(mapOf("Content-Type" to WorkflowValue.StringValue("application/json"))),
            ),
        ),
        NodeRef("outer", "json.parse"),
        NodeRef("response", "json.path", config = mapOf("path" to WorkflowValue.StringValue("response"))),
        NodeRef("parsed", "json.parse"),
        NodeRef("chainDiagnostics", ShortsNodeTypes.OLLAMA_CHAIN_DIAGNOSTICS),
    ),
    connections = listOf(
        ConnectionRef("ollama_host", "value", "ollama_url", "input"),
        ConnectionRef("ollama_model", "value", "ollama_body", "input"),
        ConnectionRef("ollama_url", "result", "request", "url"),
        ConnectionRef("body_json", "text", "request", "body"),
        ConnectionRef("ollama_body", "result", "body_json", "value"),
        ConnectionRef("request", "body", "outer", "text"),
        ConnectionRef("outer", "value", "response", "value"),
        ConnectionRef("response", "result", "parsed", "text"),
        // Diagnostic ok/error signals from every stage feed the bundling node, not each other — this
        // is what collapses the old ~9-wire fan-in into 2:1 (val + rebuild) below.
        ConnectionRef("request", "ok", "chainDiagnostics", "httpOk"),
        ConnectionRef("request", "error", "chainDiagnostics", "httpError"),
        ConnectionRef("outer", "ok", "chainDiagnostics", "outerParseOk"),
        ConnectionRef("outer", "error", "chainDiagnostics", "outerParseError"),
        ConnectionRef("response", "found", "chainDiagnostics", "responseFound"),
        ConnectionRef("parsed", "ok", "chainDiagnostics", "innerParseOk"),
        ConnectionRef("parsed", "error", "chainDiagnostics", "innerParseError"),
        // "parsed"'s "value" and "chainDiagnostics"'s "diagnostics" are deliberately left unconsumed
        // here — they're this subgraph's two free outputs, picked out by the
        // ShortsNodeTypes.OLLAMA_FETCH_SUBGRAPH wrapper executor (ShortsPlugin.kt).
    ),
)
