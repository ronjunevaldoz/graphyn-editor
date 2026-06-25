package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta

/**
 * Renders the categorised palette tree: optional parent folders (categories sharing a
 * [NodeCategoryMeta.group]) at the top level, then ungrouped categories, then any nodes with no
 * category. Folder and category expansion state is keyed in [expanded]; folder keys are prefixed
 * with "group:" so they never collide with category ids.
 */
@Composable
internal fun PaletteCategoryTree(
    grouped: Map<String?, List<NodeSpec>>,
    categories: Map<String, NodeCategoryMeta>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onAddNode: (NodeSpec) -> Unit,
) {
    val visible = categories.entries.filter { (id, _) -> grouped.containsKey(id) }
    val folders = visible.filter { it.value.group != null }.groupBy { it.value.group!! }
    val loose = visible.filter { it.value.group == null }.sortedBy { it.value.label }

    folders.entries.sortedBy { it.key }.forEach { (folderName, cats) ->
        val folderKey = "group:$folderName"
        val folderExpanded = folderKey in expanded
        PaletteFolderHeader(label = folderName, expanded = folderExpanded) { onToggle(folderKey) }
        if (folderExpanded) {
            cats.sortedBy { it.value.label }.forEach { (id, meta) ->
                CategoryBlock(id, meta, grouped, expanded, onToggle, onAddNode, indent = 12.dp)
            }
        }
    }

    loose.forEach { (id, meta) ->
        CategoryBlock(id, meta, grouped, expanded, onToggle, onAddNode, indent = 0.dp)
    }

    grouped[null].orEmpty().forEach { PaletteNodeItem(spec = it, onAdd = onAddNode) }
}

@Composable
private fun CategoryBlock(
    id: String,
    meta: NodeCategoryMeta,
    grouped: Map<String?, List<NodeSpec>>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onAddNode: (NodeSpec) -> Unit,
    indent: Dp,
) {
    val isExpanded = id in expanded
    PaletteCategoryHeader(meta = meta, expanded = isExpanded, indent = indent) { onToggle(id) }
    if (isExpanded) {
        grouped[id]?.forEach { PaletteNodeItem(spec = it, indent = indent, onAdd = onAddNode) }
    }
}
