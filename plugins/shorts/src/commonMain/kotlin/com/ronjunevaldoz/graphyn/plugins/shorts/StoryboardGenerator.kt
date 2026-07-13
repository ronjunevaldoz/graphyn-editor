package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Generates a validated storyboard `{niche, visual_style, narration, scenes: [{prompt, caption}]}`
 * for [topic] from Ollama. The Ollama request/parse chain is [ollamaFetchSubgraph], nested as one
 * `fetch` node; the final validate step is a compiled executor ([storyboardValidateExecutor],
 * `demo.storyboard.validate`), not a `script.eval` .kts script — that script reliably crashed Kotlin's
 * JSR-223 IR backend once chained after the other scripts here (a real, reproducible scripting-engine
 * bug, not a logic error). It falls back to a fixed, known-good storyboard if the LLM's JSON doesn't
 * match the expected shape, and force-unloads the Ollama model before returning (see its doc comment).
 *
 * Composes only `env.read`, `json.*`, and `io.http_request` nodes (inside [ollamaFetchSubgraph]) plus
 * this plugin's own executors, so it runs anywhere those node families are registered.
 */
public fun storyboardGeneratorSubgraph(topic: String): WorkflowDefinition = WorkflowDefinition(
    id = "storyboard-generator",
    name = "Storyboard Generator",
    nodes = listOf(
        NodeRef(
            "fetch",
            ShortsNodeTypes.OLLAMA_FETCH_SUBGRAPH,
            subgraph = ollamaFetchSubgraph(id = "storyboard-fetch", bodyNodeType = ShortsNodeTypes.OLLAMA_BODY, topic = topic),
        ),
        NodeRef("validate", ShortsNodeTypes.STORYBOARD_VALIDATE),
    ),
    connections = listOf(
        ConnectionRef("fetch", "value", "validate", "input"),
        ConnectionRef("fetch", "diagnostics", "validate", "diagnostics"),
    ),
)
