package com.ronjunevaldoz.graphyn.core.execution

interface NodeExecutorRegistry {
    fun resolve(type: String): NodeExecutor?
    fun register(type: String, executor: NodeExecutor)
}

class DefaultNodeExecutorRegistry : NodeExecutorRegistry {
    private val executors = mutableMapOf<String, NodeExecutor>()

    override fun resolve(type: String): NodeExecutor? = executors[type]

    override fun register(type: String, executor: NodeExecutor) {
        executors[type] = executor
    }
}
