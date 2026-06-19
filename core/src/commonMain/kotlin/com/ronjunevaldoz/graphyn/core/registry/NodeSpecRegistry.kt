package com.ronjunevaldoz.graphyn.core.registry

import com.ronjunevaldoz.graphyn.core.model.NodeSpec

/** Stores and resolves [NodeSpec]s by their [NodeSpec.type] string. */
interface NodeSpecRegistry {
    /** Returns the spec for [type], or null if no spec is registered. */
    fun resolve(type: String): NodeSpec?
    fun all(): List<NodeSpec>
    fun register(spec: NodeSpec)
}

/** In-memory [NodeSpecRegistry]; last-registration wins for duplicate types. */
class DefaultNodeSpecRegistry : NodeSpecRegistry {
    private val specs = mutableMapOf<String, NodeSpec>()

    override fun resolve(type: String): NodeSpec? = specs[type]

    override fun all(): List<NodeSpec> = specs.values.toList()

    override fun register(spec: NodeSpec) {
        specs[spec.type] = spec
    }
}
