package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValueFlattener
import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import com.ronjunevaldoz.graphyn.core.sync.WorkflowGraphImpact
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowDocumentCodec
import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoreWorkflowTest {
    @Test
    fun typeCompatibilityAllowsNumericWideningAndNullableWrapping() {
        assertTrue(WorkflowTypeCompatibility.isCompatible(WorkflowType.DoubleType, WorkflowType.IntType))
        assertTrue(
            WorkflowTypeCompatibility.isCompatible(
                WorkflowType.NullableType(WorkflowType.StringType),
                WorkflowType.StringType,
            ),
        )
        assertFalse(
            WorkflowTypeCompatibility.isCompatible(
                WorkflowType.IntType,
                WorkflowType.DoubleType,
            ),
        )
    }

    @Test
    fun opaqueTypeIsOnlyCompatibleWithOpaque() {
        // OpaqueType output connects to OpaqueType or NullableType(OpaqueType) inputs only.
        assertTrue(WorkflowTypeCompatibility.isCompatible(WorkflowType.OpaqueType, WorkflowType.OpaqueType))
        assertTrue(WorkflowTypeCompatibility.isCompatible(
            WorkflowType.NullableType(WorkflowType.OpaqueType), WorkflowType.OpaqueType,
        ))
        // OpaqueType does NOT flow into typed ports and typed ports do NOT flow into OpaqueType.
        assertFalse(WorkflowTypeCompatibility.isCompatible(WorkflowType.OpaqueType, WorkflowType.StringType))
        assertFalse(WorkflowTypeCompatibility.isCompatible(WorkflowType.StringType, WorkflowType.OpaqueType))
        assertFalse(WorkflowTypeCompatibility.isCompatible(
            WorkflowType.ListType(WorkflowType.IntType), WorkflowType.OpaqueType,
        ))
    }

    @Test
    fun workflowDocumentRoundTrips() {
        val workflow = WorkflowDefinition(
            id = "workflow-1",
            name = "Example",
            nodes = listOf(
                NodeRef(
                    id = "node-1",
                    type = "constant",
                    config = mapOf("value" to WorkflowValue.StringValue("hello")),
                ),
            ),
            connections = emptyList(),
        )

        val document = DefaultWorkflowDocumentCodec.encode(workflow)
        val restored = DefaultWorkflowDocumentCodec.decode(document)

        assertEquals(workflow, restored)
        assertEquals(1, document.version)
    }

    @Test
    fun workflowJsonIsReadableAndRoundTrips() {
        val workflow = WorkflowDefinition(
            id = "workflow-json",
            name = "Json",
            nodes = listOf(
                NodeRef(
                    id = "switch-1",
                    type = "switch",
                    config = mapOf("enabled" to WorkflowValue.BooleanValue(true)),
                ),
            ),
            connections = emptyList(),
        )

        val json = DefaultWorkflowJsonCodec.encodeToString(workflow)
        val restored = DefaultWorkflowJsonCodec.decodeFromString(json)

        assertTrue(json.contains("\n  "))
        assertTrue(json.contains("\"kind\""))
        assertEquals(workflow, restored)
    }

    @Test
    fun workflowValuesCanBeFlattened() {
        val flattened = WorkflowValueFlattener.flattenMap(
            mapOf(
                "switch" to WorkflowValue.RecordValue(
                    mapOf(
                        "enabled" to WorkflowValue.BooleanValue(true),
                        "meta" to WorkflowValue.RecordValue(
                            mapOf(
                                "label" to WorkflowValue.StringValue("live"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(WorkflowValue.BooleanValue(true), flattened["switch.enabled"])
        assertEquals(WorkflowValue.StringValue("live"), flattened["switch.meta.label"])
    }

    @Test
    fun validatorReportsMissingRequiredInputs() {
        val specRegistry = DefaultNodeSpecRegistry().apply {
            register(
                NodeSpec(
                    type = "printer",
                    label = "Printer",
                    inputs = listOf(
                        PortSpec(name = "text", type = WorkflowType.StringType, required = true),
                    ),
                    outputs = emptyList(),
                ),
            )
        }

        val validator = WorkflowGraphValidator(specRegistry)
        val errors = validator.validate(
            WorkflowDefinition(
                id = "workflow-2",
                name = "Broken",
                nodes = listOf(NodeRef(id = "printer-1", type = "printer")),
                connections = emptyList(),
            ),
        )

        assertTrue(errors.any { it.code == "missing_required_input" })
    }

    @Test
    fun validatorRejectsTypeIncompatibleConnections() {
        val specRegistry = DefaultNodeSpecRegistry().apply {
            register(
                NodeSpec(
                    type = "source",
                    label = "Source",
                    inputs = emptyList(),
                    outputs = listOf(
                        PortSpec(name = "value", type = WorkflowType.IntType),
                    ),
                ),
            )
            register(
                NodeSpec(
                    type = "sink",
                    label = "Sink",
                    inputs = listOf(
                        PortSpec(name = "value", type = WorkflowType.StringType),
                    ),
                    outputs = emptyList(),
                ),
            )
        }

        val validator = WorkflowGraphValidator(specRegistry)
        val errors = validator.validate(
            WorkflowDefinition(
                id = "workflow-3",
                name = "Broken Connection",
                nodes = listOf(
                    NodeRef(id = "source-1", type = "source"),
                    NodeRef(id = "sink-1", type = "sink"),
                ),
                connections = listOf(
                    ConnectionRef(
                        fromNodeId = "source-1",
                        fromPort = "value",
                        toNodeId = "sink-1",
                        toPort = "value",
                    ),
                ),
            ),
        )

        assertTrue(errors.any { it.code == "type_mismatch" })
    }

    @Test
    fun downstreamNodesAreAffectedWhenSourceNodeUpdates() {
        val workflow = WorkflowDefinition(
            id = "workflow-3",
            name = "Propagation",
            nodes = listOf(
                NodeRef(id = "switch-1", type = "switch"),
                NodeRef(id = "display-1", type = "display"),
                NodeRef(id = "detail-1", type = "detail"),
            ),
            connections = listOf(
                com.ronjunevaldoz.graphyn.core.model.ConnectionRef(
                    fromNodeId = "switch-1",
                    fromPort = "on",
                    toNodeId = "display-1",
                    toPort = "visible",
                ),
                com.ronjunevaldoz.graphyn.core.model.ConnectionRef(
                    fromNodeId = "display-1",
                    fromPort = "state",
                    toNodeId = "detail-1",
                    toPort = "input",
                ),
            ),
        )

        val dataStore = WorkflowDataStore(workflow)
        val affected = dataStore.updateNodeOutputs(
            "switch-1",
            mapOf("on" to WorkflowValue.BooleanValue(true)),
        )

        assertTrue("switch-1" in affected)
        assertTrue("display-1" in affected)
        assertTrue("detail-1" in affected)
    }

    @Test
    fun workflowExecutionResolvesExecutorsInTopologicalOrder() = kotlinx.coroutines.test.runTest {
        val specs = DefaultNodeSpecRegistry().apply {
            register(
                NodeSpec(
                    type = "switch",
                    label = "Switch",
                    inputs = listOf(
                        PortSpec(name = "enabled", type = WorkflowType.BooleanType, required = false),
                    ),
                    outputs = listOf(
                        PortSpec(name = "on", type = WorkflowType.BooleanType),
                    ),
                ),
            )
            register(
                NodeSpec(
                    type = "display",
                    label = "Display",
                    inputs = listOf(
                        PortSpec(name = "enabled", type = WorkflowType.BooleanType, required = false),
                    ),
                    outputs = listOf(
                        PortSpec(name = "state", type = WorkflowType.BooleanType),
                    ),
                ),
            )
        }
        val executors = DefaultNodeExecutorRegistry().apply {
            register("switch") { input ->
                mapOf("on" to (input["enabled"] ?: WorkflowValue.BooleanValue(false)))
            }
            register("display") { input ->
                mapOf("state" to (input["enabled"] ?: WorkflowValue.BooleanValue(false)))
            }
        }
        val engine = WorkflowExecutionEngine(executors, specs)
        val workflow = WorkflowDefinition(
            id = "workflow-exec",
            name = "Exec",
            nodes = listOf(
                NodeRef(id = "switch-1", type = "switch", config = mapOf("enabled" to WorkflowValue.BooleanValue(true))),
                NodeRef(id = "display-1", type = "display"),
            ),
            connections = listOf(
                ConnectionRef(
                    fromNodeId = "switch-1",
                    fromPort = "on",
                    toNodeId = "display-1",
                    toPort = "enabled",
                ),
            ),
        )

        val result = engine.execute(workflow)

        assertEquals(listOf("switch-1", "display-1"), result.executionOrder)
        assertEquals(
            WorkflowValue.BooleanValue(true),
            result.nodeOutputsByNodeId["display-1"]?.get("state"),
        )
    }
}
