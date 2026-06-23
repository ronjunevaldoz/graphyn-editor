package com.ronjunevaldoz.graphyn.core.model

import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

/**
 * The connectable boundary of a [WorkflowDefinition] when treated as a subgraph:
 * input ports that nothing inside fills, and output ports that nothing inside consumes.
 *
 * These are exactly the ports a subgraph node exposes to its parent — inputs receive
 * injected parent values, outputs surface the inner results.
 */
data class SubgraphBoundary(
    val inputs: List<PortSpec>,
    val outputs: List<PortSpec>,
)

/**
 * Computes the [SubgraphBoundary] of [inner] using [specs] to enumerate each node's ports.
 *
 * - An input port is *free* (exposed) when no internal connection targets it.
 * - An output port is *free* (exposed) when no internal connection consumes it.
 *
 * Ports are de-duplicated by name (first occurrence wins) so the boundary has stable,
 * unique port names — the same names the execution engine uses for input injection and
 * free-output collection.
 */
fun subgraphBoundary(inner: WorkflowDefinition, specs: NodeSpecRegistry): SubgraphBoundary {
    val filledInputs = inner.connections.mapTo(mutableSetOf()) { it.toNodeId to it.toPort }
    val consumedOutputs = inner.connections.mapTo(mutableSetOf()) { it.fromNodeId to it.fromPort }

    val inputs = linkedMapOf<String, PortSpec>()
    val outputs = linkedMapOf<String, PortSpec>()
    inner.nodes.forEach { node ->
        val spec = specs.resolve(node.type) ?: return@forEach
        spec.inputs.forEach { port ->
            if ((node.id to port.name) !in filledInputs && port.name !in inputs) inputs[port.name] = port
        }
        spec.outputs.forEach { port ->
            if ((node.id to port.name) !in consumedOutputs && port.name !in outputs) outputs[port.name] = port
        }
    }
    return SubgraphBoundary(inputs.values.toList(), outputs.values.toList())
}

/**
 * Derives a [NodeSpec] for a subgraph [node] from its embedded workflow's [subgraphBoundary].
 * Returns null when the node has no subgraph. The editor uses this so a collapsed subgraph node
 * renders its boundary ports without a statically-registered spec.
 */
fun deriveSubgraphSpec(node: NodeRef, specs: NodeSpecRegistry, label: String? = null): NodeSpec? {
    val inner = node.subgraph ?: return null
    val boundary = subgraphBoundary(inner, specs)
    return NodeSpec(
        type = node.type,
        label = label ?: inner.name,
        inputs = boundary.inputs,
        outputs = boundary.outputs,
        description = "Subgraph: ${inner.nodes.size} nodes",
    )
}
