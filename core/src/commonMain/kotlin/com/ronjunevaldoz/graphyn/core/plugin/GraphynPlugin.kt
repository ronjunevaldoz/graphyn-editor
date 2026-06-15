package com.ronjunevaldoz.graphyn.core.plugin

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutorRegistry

interface GraphynPlugin {
    val id: String
    val displayName: String

    fun register(context: GraphynPluginContext)
}

interface GraphynPluginContext {
    val nodeSpecs: NodeSpecRegistry
    val nodeExecutors: NodeExecutorRegistry

    fun registerNodeSpec(spec: NodeSpec) {
        nodeSpecs.register(spec)
    }

    fun registerExecutor(type: String, executor: NodeExecutor) {
        nodeExecutors.register(type, executor)
    }
}

class DefaultGraphynPluginContext(
    override val nodeSpecs: NodeSpecRegistry,
    override val nodeExecutors: NodeExecutorRegistry,
) : GraphynPluginContext
