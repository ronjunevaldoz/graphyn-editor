package com.ronjunevaldoz.graphyn.core.execution

/** Maps node type strings to their [NodeExecutor] implementations at runtime. */
interface NodeExecutorRegistry {
    /** Returns the executor registered for [type], or null if none is registered. */
    fun resolve(type: String): NodeExecutor?
    fun register(type: String, executor: NodeExecutor)
}

/** In-memory [NodeExecutorRegistry] backed by a mutable map; last-registration wins. */
class DefaultNodeExecutorRegistry : NodeExecutorRegistry {
    private val executors = mutableMapOf<String, NodeExecutor>()

    override fun resolve(type: String): NodeExecutor? = executors[type]

    override fun register(type: String, executor: NodeExecutor) {
        executors[type] = executor
    }
}
