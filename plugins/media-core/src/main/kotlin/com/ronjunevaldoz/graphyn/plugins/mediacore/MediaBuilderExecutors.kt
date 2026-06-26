package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Executor factories for the in-graph record builders registered by [MediaCorePlugin]. */
internal fun videoOverlayExecutor() = NodeExecutor { inputs ->
    val source = inputs["source"] ?: error("Video Overlay requires a source video.")
    MediaTypes.path(source, "video") // validate the handle up front
    mapOf(
        "overlay" to WorkflowValue.RecordValue(
            mapOf(
                "source" to source,
                "x" to WorkflowValue.IntValue(inputs.intOr("x", 0)),
                "y" to WorkflowValue.IntValue(inputs.intOr("y", 0)),
                "start_ms" to WorkflowValue.DoubleValue(inputs.numberOr("start_ms", 0.0)),
                "end_ms" to WorkflowValue.DoubleValue(inputs.numberOr("end_ms", 0.0)),
                "opacity" to WorkflowValue.DoubleValue(inputs.numberOr("opacity", 1.0)),
            ),
        ),
    )
}

internal fun syncPointExecutor() = NodeExecutor { inputs ->
    mapOf(
        "point" to WorkflowValue.RecordValue(
            mapOf(
                "source_ms" to WorkflowValue.DoubleValue(inputs.numberOr("source_ms", 0.0)),
                "target_ms" to WorkflowValue.DoubleValue(inputs.numberOr("target_ms", 0.0)),
            ),
        ),
    )
}

internal fun recordListExecutor(prefix: String, outputName: String, label: String) = NodeExecutor { inputs ->
    val items = (1..4).mapNotNull { index ->
        inputs["$prefix$index"]?.takeUnless { it == WorkflowValue.NullValue }
    }
    require(items.isNotEmpty()) { "$label requires at least one item." }
    items.forEach { require(it is WorkflowValue.RecordValue) { "$label inputs must be records." } }
    mapOf(outputName to WorkflowValue.ListValue(items))
}

private fun Map<String, WorkflowValue>.intOr(key: String, default: Int): Int = when (val value = this[key]) {
    is WorkflowValue.IntValue -> value.value
    is WorkflowValue.DoubleValue -> value.value.toInt()
    else -> default
}
