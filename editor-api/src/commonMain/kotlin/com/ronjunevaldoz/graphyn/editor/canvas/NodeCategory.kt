package com.ronjunevaldoz.graphyn.editor.canvas

/**
 * Display metadata for a node category shown in the node-picker palette.
 *
 * @param label Human-readable category name shown above the group in the palette.
 * @param color ARGB color used to tint the category header and node type badges.
 * @param icon Optional emoji or single-character icon rendered next to [label].
 */
data class NodeCategoryMeta(
    val label: String,
    val color: Long,
    val icon: String = "",
)

/**
 * Maps category id strings to their [NodeCategoryMeta] for palette grouping.
 *
 * Register categories via [GraphynEditorPluginRegistrar.registerCategory].
 */
interface NodeCategoryRegistry {
    /** Returns the meta for [categoryId], or null if it has not been registered. */
    fun resolve(categoryId: String): NodeCategoryMeta?
    /** Associates [meta] with [categoryId]. Overwrites any previous registration. */
    fun register(categoryId: String, meta: NodeCategoryMeta)
    /** Returns all registered category id → meta pairs. */
    fun all(): Map<String, NodeCategoryMeta>
}

/** In-memory [NodeCategoryRegistry]. */
class DefaultNodeCategoryRegistry : NodeCategoryRegistry {
    private val categories = mutableMapOf<String, NodeCategoryMeta>()
    override fun resolve(categoryId: String): NodeCategoryMeta? = categories[categoryId]
    override fun register(categoryId: String, meta: NodeCategoryMeta) { categories[categoryId] = meta }
    override fun all(): Map<String, NodeCategoryMeta> = categories.toMap()
}
