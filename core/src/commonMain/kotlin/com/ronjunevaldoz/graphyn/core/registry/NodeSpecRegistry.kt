package com.ronjunevaldoz.graphyn.core.registry

import com.ronjunevaldoz.graphyn.core.model.NodeSpec

interface NodeSpecRegistry {
    fun resolve(type: String): NodeSpec?
    fun all(): List<NodeSpec>
    fun register(spec: NodeSpec)
}

class DefaultNodeSpecRegistry : NodeSpecRegistry {
    private val specs = mutableMapOf<String, NodeSpec>()

    override fun resolve(type: String): NodeSpec? = specs[type]

    override fun all(): List<NodeSpec> = specs.values.toList()

    override fun register(spec: NodeSpec) {
        specs[spec.type] = spec
    }
}
