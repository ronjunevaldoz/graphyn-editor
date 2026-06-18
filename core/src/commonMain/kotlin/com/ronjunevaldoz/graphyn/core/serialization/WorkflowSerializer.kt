package com.ronjunevaldoz.graphyn.core.serialization

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

fun WorkflowDefinition.toJson(codec: WorkflowJsonCodec = DefaultWorkflowJsonCodec): String =
    codec.encodeToString(this)

fun workflowFromJson(
    json: String,
    codec: WorkflowJsonCodec = DefaultWorkflowJsonCodec,
): WorkflowDefinition = codec.decodeFromString(json)
