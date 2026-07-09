package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Generates a validated comparison arc `{niche, visual_style, narration, pairs: [{label_a,
 * label_b, prompt_a, prompt_b, question, answer}]}` for [topic] from Ollama. Sibling to
 * [storyboardGeneratorSubgraph] — same shape, reuses [ollamaUrlExecutor]/[ollamaUrlSpec] as-is
 * (URL-building doesn't depend on the prompt), swaps in [comparisonOllamaBodySpec]/
 * [comparisonValidateExecutor] for the different JSON schema. See that function's doc comment for
 * why this composes only `env.read`, `json.*`, `io.http_request`, and this plugin's compiled
 * executors instead of `script.eval`.
 */
public fun comparisonGeneratorSubgraph(topic: String): WorkflowDefinition = WorkflowDefinition(
    id = "comparison-generator",
    name = "Comparison Generator",
    nodes = listOf(
        NodeRef("ollama_host", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_HOST"))),
        NodeRef("ollama_model", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_MODEL"))),
        NodeRef("ollama_url", ShortsNodeTypes.OLLAMA_URL),
        NodeRef("ollama_body", ShortsNodeTypes.COMPARISON_OLLAMA_BODY, config = mapOf("topic" to WorkflowValue.StringValue(topic))),
        NodeRef("body_json", "json.stringify"),
        NodeRef("request", "io.http_request", config = mapOf(
            "method" to WorkflowValue.StringValue("POST"),
            "headers" to WorkflowValue.RecordValue(mapOf("Content-Type" to WorkflowValue.StringValue("application/json"))),
        )),
        NodeRef("outer", "json.parse"),
        NodeRef("response", "json.path", config = mapOf("path" to WorkflowValue.StringValue("response"))),
        NodeRef("comparisonJson", "json.parse"),
        NodeRef("validate", ShortsNodeTypes.COMPARISON_VALIDATE),
    ),
    connections = listOf(
        ConnectionRef("ollama_host", "value", "ollama_url", "input"),
        ConnectionRef("ollama_model", "value", "ollama_body", "input"),
        ConnectionRef("ollama_url", "result", "request", "url"),
        ConnectionRef("body_json", "text", "request", "body"),
        ConnectionRef("ollama_body", "result", "body_json", "value"),
        ConnectionRef("request", "body", "outer", "text"),
        ConnectionRef("outer", "value", "response", "value"),
        ConnectionRef("response", "result", "comparisonJson", "text"),
        ConnectionRef("comparisonJson", "value", "validate", "input"),
        ConnectionRef("request", "ok", "validate", "httpOk"),
        ConnectionRef("request", "error", "validate", "httpError"),
        ConnectionRef("outer", "ok", "validate", "outerParseOk"),
        ConnectionRef("outer", "error", "validate", "outerParseError"),
        ConnectionRef("response", "found", "validate", "responseFound"),
        ConnectionRef("comparisonJson", "ok", "validate", "innerParseOk"),
        ConnectionRef("comparisonJson", "error", "validate", "innerParseError"),
    ),
)
