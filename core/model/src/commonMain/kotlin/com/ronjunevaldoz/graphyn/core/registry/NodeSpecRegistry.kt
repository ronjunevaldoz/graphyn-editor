package com.ronjunevaldoz.graphyn.core.registry

import com.ronjunevaldoz.graphyn.core.model.NodeSpec

/** Stores and resolves [NodeSpec]s by their [NodeSpec.type] string. */
interface NodeSpecRegistry {
    /** Returns the spec for [type], or null if no spec is registered. */
    fun resolve(type: String): NodeSpec?
    /** Returns all registered specs in registration order. */
    fun all(): List<NodeSpec>
    /** Registers [spec]; overwrites any existing registration for the same [NodeSpec.type]. */
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
