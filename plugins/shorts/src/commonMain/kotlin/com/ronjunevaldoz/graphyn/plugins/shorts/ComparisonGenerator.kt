package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Generates a validated comparison arc `{niche, visual_style, narration, pairs: [{label_a,
 * label_b, prompt_a, prompt_b, question, answer}]}` for [topic] from Ollama. Sibling to
 * [storyboardGeneratorSubgraph] — same shape, reuses [ollamaFetchSubgraph] as-is (URL-building and
 * the request/parse chain don't depend on the prompt) with [ShortsNodeTypes.COMPARISON_OLLAMA_BODY]
 * swapped in for the different JSON schema, then [comparisonValidateExecutor]. See
 * [storyboardGeneratorSubgraph]'s doc comment for why this composes only `env.read`, `json.*`,
 * `io.http_request`, and this plugin's compiled executors instead of `script.eval`.
 */
public fun comparisonGeneratorSubgraph(topic: String): WorkflowDefinition = WorkflowDefinition(
    id = "comparison-generator",
    name = "Comparison Generator",
    nodes = listOf(
        NodeRef(
            "fetch",
            ShortsNodeTypes.OLLAMA_FETCH_SUBGRAPH,
            subgraph = ollamaFetchSubgraph(id = "comparison-fetch", bodyNodeType = ShortsNodeTypes.COMPARISON_OLLAMA_BODY, topic = topic),
        ),
        NodeRef("validate", ShortsNodeTypes.COMPARISON_VALIDATE),
    ),
    connections = listOf(
        ConnectionRef("fetch", "value", "validate", "input"),
        ConnectionRef("fetch", "diagnostics", "validate", "diagnostics"),
    ),
)
