package com.ronjunevaldoz.graphyn.core.serialization

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/**
 * Serialize this workflow to a pretty-printed JSON string.
 *
 * The output format is versioned ([GRAPHYN_WORKFLOW_FORMAT_VERSION]) and forward-compatible:
 * unknown keys added by future versions are silently ignored on decode.
 *
 * Supply a custom [codec] to override encoding behaviour (e.g. compact output, custom Json instance).
 */
fun WorkflowDefinition.toJson(codec: WorkflowJsonCodec = DefaultWorkflowJsonCodec): String =
    codec.encodeToString(this)

/**
 * Deserialize a workflow from a JSON string produced by [toJson].
 *
 * Throws [kotlinx.serialization.SerializationException] if the JSON is structurally invalid.
 * Unknown keys from future format versions are silently ignored.
 */
fun workflowFromJson(
    json: String,
    codec: WorkflowJsonCodec = DefaultWorkflowJsonCodec,
): WorkflowDefinition = codec.decodeFromString(json)
