package com.ronjunevaldoz.graphyn.plugins.math

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MathPluginTest {
    @Test
    fun mathPluginRegistersThreeNodeSpecs() {
        val registry = DefaultGraphynPluginRegistry()
        registry.install(MathPlugin)
        assertEquals(3, registry.nodeSpecs.all().size)
        assertNotNull(registry.nodeSpecs.resolve("math.add"))
        assertNotNull(registry.nodeSpecs.resolve("math.subtract"))
        assertNotNull(registry.nodeSpecs.resolve("math.multiply"))
    }

    @Test
    fun addExecutorComputesSum() {
        val result = MathExecutors.add(
            mapOf(
                "left" to WorkflowValue.DoubleValue(3.0),
                "right" to WorkflowValue.DoubleValue(4.0),
            ),
        )
        assertEquals(WorkflowValue.DoubleValue(7.0), result["result"])
    }

    @Test
    fun subtractExecutorComputesDifference() {
        val result = MathExecutors.subtract(
            mapOf(
                "left" to WorkflowValue.DoubleValue(10.0),
                "right" to WorkflowValue.DoubleValue(4.0),
            ),
        )
        assertEquals(WorkflowValue.DoubleValue(6.0), result["result"])
    }

    @Test
    fun multiplyExecutorComputesProduct() {
        val result = MathExecutors.multiply(
            mapOf(
                "left" to WorkflowValue.DoubleValue(3.0),
                "right" to WorkflowValue.DoubleValue(5.0),
            ),
        )
        assertEquals(WorkflowValue.DoubleValue(15.0), result["result"])
    }

    @Test
    fun mathExecutorUsesZeroDefaultForMissingInputs() {
        val result = MathExecutors.add(emptyMap())
        assertEquals(WorkflowValue.DoubleValue(0.0), result["result"])
    }

    @Test
    fun mathWorkflowExecutesChainedOperations() {
        // add(2, 3) = 5  →  multiply(5, 4) = 20
        val specs = DefaultNodeSpecRegistry()
        val executors = DefaultNodeExecutorRegistry()
        DefaultGraphynPluginRegistry(specs, executors).install(MathPlugin)

        val workflow = WorkflowDefinition(
            id = "math-chain",
            name = "Math Chain",
            nodes = listOf(
                NodeRef("add-1", "math.add", config = mapOf(
                    "left" to WorkflowValue.DoubleValue(2.0),
                    "right" to WorkflowValue.DoubleValue(3.0),
                )),
                NodeRef("mul-1", "math.multiply", config = mapOf(
                    "right" to WorkflowValue.DoubleValue(4.0),
                )),
            ),
            connections = listOf(
                ConnectionRef(fromNodeId = "add-1", fromPort = "result", toNodeId = "mul-1", toPort = "left"),
            ),
        )

        val result = WorkflowExecutionEngine(executors, specs).execute(workflow)

        assertEquals(
            WorkflowValue.DoubleValue(20.0),
            result.nodeOutputsByNodeId["mul-1"]?.get("result"),
        )
    }
}
