package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import com.ronjunevaldoz.graphyn.core.serialization.GRAPHYN_WORKFLOW_FORMAT_VERSION
import com.ronjunevaldoz.graphyn.core.serialization.toJson
import com.ronjunevaldoz.graphyn.core.serialization.workflowFromJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowSerializerTest {

    private fun minimal() = WorkflowDefinition(
        id = "w-1", name = "Test", nodes = emptyList(), connections = emptyList(),
    )

    private fun allValueTypes() = WorkflowDefinition(
        id = "w-types",
        name = "All Types",
        nodes = listOf(
            NodeRef(
                id = "n-1",
                type = "probe",
                config = mapOf(
                    "str"    to WorkflowValue.StringValue("hello"),
                    "int"    to WorkflowValue.IntValue(42),
                    "double" to WorkflowValue.DoubleValue(3.14),
                    "bool"   to WorkflowValue.BooleanValue(true),
                    "null"   to WorkflowValue.NullValue,
                    "list"   to WorkflowValue.ListValue(listOf(
                        WorkflowValue.IntValue(1),
                        WorkflowValue.IntValue(2),
                    )),
                    "record" to WorkflowValue.RecordValue(mapOf(
                        "nested" to WorkflowValue.StringValue("deep"),
                    )),
                ),
            ),
        ),
        connections = emptyList(),
    )

    @Test
    fun facadeRoundTrip() {
        val workflow = allValueTypes()
        assertEquals(workflow, workflowFromJson(workflow.toJson()))
    }

    @Test
    fun allValueTypesPreservedAfterRoundTrip() {
        val restored = workflowFromJson(allValueTypes().toJson())
        val config = restored.nodes.first().config

        assertEquals(WorkflowValue.StringValue("hello"),          config["str"])
        assertEquals(WorkflowValue.IntValue(42),                  config["int"])
        assertEquals(WorkflowValue.DoubleValue(3.14),             config["double"])
        assertEquals(WorkflowValue.BooleanValue(true),            config["bool"])
        assertEquals(WorkflowValue.NullValue,                     config["null"])
        assertEquals(WorkflowValue.ListValue(listOf(
            WorkflowValue.IntValue(1), WorkflowValue.IntValue(2))), config["list"])
        assertEquals(WorkflowValue.RecordValue(mapOf(
            "nested" to WorkflowValue.StringValue("deep"))),      config["record"])
    }

    @Test
    fun emptyWorkflowRoundTrip() {
        val workflow = minimal()
        assertEquals(workflow, workflowFromJson(workflow.toJson()))
    }

    @Test
    fun connectionsFidelity() {
        val workflow = WorkflowDefinition(
            id = "w-conn", name = "Connections",
            nodes = listOf(
                NodeRef(id = "a", type = "source"),
                NodeRef(id = "b", type = "sink"),
            ),
            connections = listOf(
                ConnectionRef("a", "out", "b", "in"),
                ConnectionRef("a", "log", "b", "debug"),
            ),
        )
        val restored = workflowFromJson(workflow.toJson())
        assertEquals(workflow.connections, restored.connections)
    }

    @Test
    fun nodePositionsSurviveRoundTrip() {
        val workflow = minimal().copy(
            nodePositions = mapOf(
                "source" to WorkflowNodePosition(120, 240),
                "sink" to WorkflowNodePosition(640, 240),
            ),
        )

        assertEquals(workflow.nodePositions, workflowFromJson(workflow.toJson()).nodePositions)
    }

    @Test
    fun unknownJsonKeysAreIgnoredOnDecode() {
        val json = workflow.toJson().replace(
            "\"version\"",
            "\"futureField\": \"ignored\", \"version\"",
        )
        val restored = workflowFromJson(json)
        assertEquals(workflow.id, restored.id)
    }

    @Test
    fun jsonOutputIsHumanReadable() {
        val json = minimal().toJson()
        assertTrue(json.contains('\n'), "Expected pretty-printed newlines")
        assertTrue(json.contains("  "),  "Expected indentation")
    }

    @Test
    fun versionFieldPresent() {
        val json = minimal().toJson()
        assertTrue(json.contains("\"version\""))
        assertTrue(json.contains(GRAPHYN_WORKFLOW_FORMAT_VERSION.toString()))
    }

    @Test
    fun largeWorkflowRoundTrip() {
        val nodes = (1..200).map { i ->
            NodeRef(id = "node-$i", type = "op", config = mapOf(
                "index" to WorkflowValue.IntValue(i),
                "label" to WorkflowValue.StringValue("Node $i"),
            ))
        }
        val connections = (1 until 200).map { i ->
            ConnectionRef("node-$i", "out", "node-${i + 1}", "in")
        }
        val large = WorkflowDefinition("large", "Large", nodes, connections)
        val restored = workflowFromJson(large.toJson())
        assertEquals(large.nodes.size,       restored.nodes.size)
        assertEquals(large.connections.size, restored.connections.size)
        assertEquals(large,                  restored)
    }

    private val workflow = minimal()
}
