package com.ronjunevaldoz.graphyn.core.benchmark

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValueFlattener
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import com.ronjunevaldoz.graphyn.core.sync.WorkflowGraphImpact
import kotlin.system.measureNanoTime

private const val WARMUP_ITERATIONS = 10
private const val SAMPLE_ITERATIONS = 50
private const val WORKFLOW_SAMPLE_NODES = 96
private const val FLATTEN_SAMPLE_WIDTH = 24

data class BenchmarkResult(
    val name: String,
    val averageMicros: Double,
    val minMicros: Double,
    val maxMicros: Double,
)

fun main() {
    val workflow = sampleWorkflow()
    val nestedValues = sampleNestedValues()

    val results = listOf(
        benchmark("json_roundtrip") {
            val encoded = DefaultWorkflowJsonCodec.encodeToString(workflow)
            DefaultWorkflowJsonCodec.decodeFromString(encoded)
        },
        benchmark("flatten_outputs") {
            WorkflowValueFlattener.flattenMap(nestedValues)
        },
        benchmark("graph_impact") {
            WorkflowGraphImpact.affectedNodeIds(workflow, "node-1")
        },
    )

    println("Graphyn core benchmark snapshot")
    println("workload,nodes,average_us,min_us,max_us")
    results.forEach { result ->
        println(
            "${result.name},${WORKFLOW_SAMPLE_NODES},${format(result.averageMicros)}," +
                "${format(result.minMicros)},${format(result.maxMicros)}",
        )
    }
    println("note,These are local snapshot measurements from the current workspace machine.")
}

private fun benchmark(name: String, action: () -> Unit): BenchmarkResult {
    repeat(WARMUP_ITERATIONS) { action() }

    val samples = DoubleArray(SAMPLE_ITERATIONS)
    repeat(SAMPLE_ITERATIONS) { index ->
        samples[index] = measureNanoTime { action() } / 1_000.0
    }

    val sorted = samples.sorted()
    return BenchmarkResult(
        name = name,
        averageMicros = samples.average(),
        minMicros = sorted.first(),
        maxMicros = sorted.last(),
    )
}

private fun sampleWorkflow(): WorkflowDefinition {
    val nodes = (1..WORKFLOW_SAMPLE_NODES).map { index ->
        NodeRef(
            id = "node-$index",
            type = if (index % 2 == 0) "math.add" else "value.constant",
            config = mapOf(
                "name" to WorkflowValue.StringValue("Node $index"),
                "enabled" to WorkflowValue.BooleanValue(index % 3 != 0),
                "count" to WorkflowValue.IntValue(index),
            ),
        )
    }
    val connections = (1 until WORKFLOW_SAMPLE_NODES).map { index ->
        ConnectionRef(
            fromNodeId = "node-$index",
            fromPort = "out",
            toNodeId = "node-${index + 1}",
            toPort = "in",
        )
    }

    return WorkflowDefinition(
        id = "benchmark-workflow",
        name = "Benchmark Workflow",
        nodes = nodes,
        connections = connections,
    )
}

private fun sampleNestedValues(): Map<String, WorkflowValue> {
    val record = (1..FLATTEN_SAMPLE_WIDTH).associate { index ->
        "field$index" to WorkflowValue.ListValue(
            listOf(
                WorkflowValue.IntValue(index),
                WorkflowValue.RecordValue(
                    mapOf(
                        "label" to WorkflowValue.StringValue("value-$index"),
                        "flag" to WorkflowValue.BooleanValue(index % 2 == 0),
                    ),
                ),
            ),
        )
    }
    return mapOf("payload" to WorkflowValue.RecordValue(record))
}

private fun format(value: Double): String = String.format("%.2f", value)
