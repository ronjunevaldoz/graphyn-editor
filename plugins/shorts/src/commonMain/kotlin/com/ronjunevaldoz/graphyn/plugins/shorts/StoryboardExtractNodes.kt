package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr

/** Extracts a single top-level string field from a validated storyboard record. */
public val storyboardFieldSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.STORYBOARD_FIELD, label = "Storyboard Field", category = ShortsConstants.CATEGORY,
    description = "Extracts a single string field from a validated storyboard record.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false), PortSpec("field", WorkflowType.StringType)),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)

/** Executor for [storyboardFieldSpec]. */
public val storyboardFieldExecutor: NodeExecutor = NodeExecutor { inputs ->
    val field = inputs.stringOr("field", "")
    val record = (inputs["input"] as? WorkflowValue.RecordValue)?.fields
    val value = (record?.get(field) as? WorkflowValue.StringValue)?.value.orEmpty()
    mapOf("result" to WorkflowValue.StringValue(value))
}

/** Extracts one field of one scene (by index) from a validated storyboard record. */
public val storyboardSceneFieldSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.STORYBOARD_SCENE_FIELD, label = "Storyboard Scene Field", category = ShortsConstants.CATEGORY,
    description = "Extracts one field of one scene (by index) from a validated storyboard record.",
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
        PortSpec("index", WorkflowType.IntType),
        PortSpec("field", WorkflowType.StringType),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)

/** Executor for [storyboardSceneFieldSpec]. */
public val storyboardSceneFieldExecutor: NodeExecutor = NodeExecutor { inputs ->
    val index = (inputs["index"] as? WorkflowValue.IntValue)?.value ?: 0
    val field = inputs.stringOr("field", "")
    val scenes = (inputs["input"] as? WorkflowValue.RecordValue)?.fields?.get("scenes") as? WorkflowValue.ListValue
    val scene = scenes?.items?.getOrNull(index) as? WorkflowValue.RecordValue
    val value = (scene?.fields?.get(field) as? WorkflowValue.StringValue)?.value.orEmpty()
    mapOf("result" to WorkflowValue.StringValue(value))
}

/** Builds caption records (text/start_ms/end_ms) from a validated storyboard's scenes. */
public val storyboardCaptionsSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.STORYBOARD_CAPTIONS, label = "Storyboard Captions", category = ShortsConstants.CATEGORY,
    description = "Builds caption records (text/start_ms/end_ms) from a validated storyboard's scenes.",
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
        PortSpec("scene_duration_ms", WorkflowType.DoubleType),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType)),
)

/** Executor for [storyboardCaptionsSpec]. */
public val storyboardCaptionsExecutor: NodeExecutor = NodeExecutor { inputs ->
    val sceneDurationMs = (inputs["scene_duration_ms"] as? WorkflowValue.DoubleValue)?.value ?: 2000.0
    val scenes = (inputs["input"] as? WorkflowValue.RecordValue)?.fields?.get("scenes") as? WorkflowValue.ListValue
    val captions = scenes?.items?.mapIndexed { index, scene ->
        val caption = ((scene as? WorkflowValue.RecordValue)?.fields?.get("caption") as? WorkflowValue.StringValue)?.value.orEmpty()
        WorkflowValue.RecordValue(
            mapOf(
                "text" to WorkflowValue.StringValue(caption),
                "start_ms" to WorkflowValue.DoubleValue(index * sceneDurationMs),
                "end_ms" to WorkflowValue.DoubleValue((index + 1) * sceneDurationMs),
            ),
        )
    }.orEmpty()
    mapOf("result" to WorkflowValue.ListValue(captions))
}
