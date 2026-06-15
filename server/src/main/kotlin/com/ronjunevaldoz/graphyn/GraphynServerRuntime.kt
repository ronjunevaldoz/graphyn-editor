package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.DefaultNodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutorRegistry
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

data class GraphynServerRuntime(
    val nodeSpecs: NodeSpecRegistry,
    val nodeExecutors: NodeExecutorRegistry,
    val executionEngine: WorkflowExecutionEngine,
)

fun createGraphynServerRuntime(): GraphynServerRuntime {
    val nodeSpecs = DefaultNodeSpecRegistry()
    val nodeExecutors = DefaultNodeExecutorRegistry()

    registerBuiltIns(nodeSpecs, nodeExecutors)

    return GraphynServerRuntime(
        nodeSpecs = nodeSpecs,
        nodeExecutors = nodeExecutors,
        executionEngine = WorkflowExecutionEngine(
            nodeExecutors = nodeExecutors,
            nodeSpecs = nodeSpecs,
        ),
    )
}

private fun registerBuiltIns(
    nodeSpecs: NodeSpecRegistry,
    nodeExecutors: DefaultNodeExecutorRegistry,
) {
    nodeSpecs.register(
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
    nodeExecutors.register("switch") { input ->
        val enabled = input["enabled"] as? WorkflowValue.BooleanValue ?: WorkflowValue.BooleanValue(false)
        mapOf("on" to enabled)
    }

    nodeSpecs.register(
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
    nodeExecutors.register("display") { input ->
        val enabled = input["enabled"] as? WorkflowValue.BooleanValue ?: WorkflowValue.BooleanValue(false)
        mapOf("state" to enabled)
    }

    nodeSpecs.register(
        NodeSpec(
            type = "value.constant",
            label = "Constant",
            inputs = emptyList(),
            outputs = listOf(
                PortSpec(name = "value", type = WorkflowType.OpaqueType, required = false),
            ),
        ),
    )
    nodeExecutors.register("value.constant") { input ->
        mapOf("value" to (input["value"] ?: WorkflowValue.NullValue))
    }
}
