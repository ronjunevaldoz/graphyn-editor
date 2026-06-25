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
private const val WORKFLOW_LARGE_NODES = 500
private const val FLATTEN_SAMPLE_WIDTH = 24

data class BenchmarkResult(
    val name: String,
    val averageMicros: Double,
    val minMicros: Double,
    val maxMicros: Double,
)

fun main() {
    val workflow = sampleWorkflow()
    val largeWorkflow = sampleWorkflow(WORKFLOW_LARGE_NODES)
    val nestedValues = sampleNestedValues()
    val encodedWorkflow = DefaultWorkflowJsonCodec.encodeToString(workflow)
    val encodedLarge = DefaultWorkflowJsonCodec.encodeToString(largeWorkflow)

    data class Row(val result: BenchmarkResult, val nodes: Int)

    val rows = listOf(
        Row(benchmark("json_encode")        { DefaultWorkflowJsonCodec.encodeToString(workflow) },        WORKFLOW_SAMPLE_NODES),
        Row(benchmark("json_decode")        { DefaultWorkflowJsonCodec.decodeFromString(encodedWorkflow) }, WORKFLOW_SAMPLE_NODES),
        Row(benchmark("json_encode_large")  { DefaultWorkflowJsonCodec.encodeToString(largeWorkflow) },   WORKFLOW_LARGE_NODES),
        Row(benchmark("json_decode_large")  { DefaultWorkflowJsonCodec.decodeFromString(encodedLarge) },  WORKFLOW_LARGE_NODES),
        Row(benchmark("flatten_outputs")    { WorkflowValueFlattener.flattenMap(nestedValues) },           FLATTEN_SAMPLE_WIDTH),
        Row(benchmark("graph_impact")       { WorkflowGraphImpact.affectedNodeIds(workflow, "node-1") },  WORKFLOW_SAMPLE_NODES),
    )

    println("Graphyn core benchmark snapshot")
    println("workload,nodes,average_us,min_us,max_us")
    rows.forEach { (result, nodes) ->
        println("${result.name},$nodes,${format(result.averageMicros)},${format(result.minMicros)},${format(result.maxMicros)}")
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

private fun sampleWorkflow(nodeCount: Int = WORKFLOW_SAMPLE_NODES): WorkflowDefinition {
    val nodes = (1..nodeCount).map { index ->
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
    val connections = (1 until nodeCount).map { index ->
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
