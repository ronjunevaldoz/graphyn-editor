package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val STORYBOARD_SCENE_COUNT = 3

/**
 * Generates a validated storyboard `{niche, visual_style, narration, scenes: [{prompt, caption}]}`
 * for [topic] from Ollama. The final validate step is a compiled executor ([storyboardValidateExecutor],
 * `demo.storyboard.validate`), not a `script.eval` .kts script — that script reliably crashed Kotlin's
 * JSR-223 IR backend once chained after the other scripts here (a real, reproducible scripting-engine
 * bug, not a logic error). It falls back to a fixed, known-good storyboard if the LLM's JSON doesn't
 * match the expected shape, and force-unloads the Ollama model before returning (see its doc comment).
 */
internal fun storyboardGeneratorSubgraph(topic: String) = WorkflowDefinition(
    id = "storyboard-generator",
    name = "Storyboard Generator",
    nodes = listOf(
        NodeRef("ollama_host", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_HOST"))),
        NodeRef("ollama_model", "env.read", config = mapOf("name" to WorkflowValue.StringValue("GRAPHYN_OLLAMA_MODEL"))),
        NodeRef("ollama_url", OLLAMA_URL_NODE_TYPE),
        NodeRef("ollama_body", OLLAMA_BODY_NODE_TYPE, config = mapOf("topic" to WorkflowValue.StringValue(topic))),
        NodeRef("body_json", "json.stringify"),
        NodeRef("request", "io.http_request", config = mapOf(
            "method" to WorkflowValue.StringValue("POST"),
            "headers" to WorkflowValue.RecordValue(mapOf("Content-Type" to WorkflowValue.StringValue("application/json"))),
        )),
        NodeRef("outer", "json.parse"),
        NodeRef("response", "json.path", config = mapOf("path" to WorkflowValue.StringValue("response"))),
        NodeRef("storyboardJson", "json.parse"),
        NodeRef("validate", STORYBOARD_VALIDATE_NODE_TYPE),
    ),
    connections = listOf(
        ConnectionRef("ollama_host", "value", "ollama_url", "input"),
        ConnectionRef("ollama_model", "value", "ollama_body", "input"),
        ConnectionRef("ollama_url", "result", "request", "url"),
        ConnectionRef("body_json", "text", "request", "body"),
        ConnectionRef("ollama_body", "result", "body_json", "value"),
        ConnectionRef("request", "body", "outer", "text"),
        ConnectionRef("outer", "value", "response", "value"),
        ConnectionRef("response", "result", "storyboardJson", "text"),
        ConnectionRef("storyboardJson", "value", "validate", "input"),
    ),
)
