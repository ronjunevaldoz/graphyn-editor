package com.ronjunevaldoz.graphyn.editor.canvas

/** Display metadata for a node category shown in the node-picker palette. */
data class NodeCategoryMeta(
    val label: String,
    val color: Long,
    val icon: String = "",
)

/** Maps category id strings to their [NodeCategoryMeta] for palette grouping. */
interface NodeCategoryRegistry {
    fun resolve(categoryId: String): NodeCategoryMeta?
    fun register(categoryId: String, meta: NodeCategoryMeta)
    fun all(): Map<String, NodeCategoryMeta>
}

class DefaultNodeCategoryRegistry : NodeCategoryRegistry {
    private val categories = mutableMapOf<String, NodeCategoryMeta>()
    override fun resolve(categoryId: String): NodeCategoryMeta? = categories[categoryId]
    override fun register(categoryId: String, meta: NodeCategoryMeta) { categories[categoryId] = meta }
    override fun all(): Map<String, NodeCategoryMeta> = categories.toMap()
}
