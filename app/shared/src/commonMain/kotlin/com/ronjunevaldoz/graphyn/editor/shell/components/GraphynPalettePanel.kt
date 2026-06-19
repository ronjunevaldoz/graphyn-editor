package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryRegistry
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun GraphynPalettePanel(
    modifier: Modifier,
    nodeSpecs: NodeSpecRegistry,
    categoryRegistry: NodeCategoryRegistry?,
    onAddNode: (NodeSpec) -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(emptySet<String>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panelBackground)
            .border(width = 1.dp, color = colors.border),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicText(text = "NODES", style = type.panelTitle.copy(color = colors.textSecondary))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceCard)
                .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.testTag("palette-search"),
                textStyle = type.bodySmall.copy(color = colors.textPrimary),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) BasicText("Search nodes…", style = type.bodySmall.copy(color = colors.textDisabled))
                    inner()
                },
            )
        }

        val allSpecs = nodeSpecs.all()
        val filtered = if (query.isBlank()) allSpecs else allSpecs.filter {
            it.label.contains(query, ignoreCase = true) || it.type.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText(
                    text = if (allSpecs.isEmpty()) "No nodes\nregistered yet." else "No results for\n\"$query\"",
                    style = type.bodySmall.copy(color = colors.textDisabled),
                )
            }
            return@Column
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
            if (query.isNotBlank() || categoryRegistry == null) {
                filtered.forEach { PaletteNodeItem(spec = it, onAdd = onAddNode) }
            } else {
                val grouped = filtered.groupBy { it.category }
                val uncategorized = grouped[null].orEmpty()
                val allCategories = categoryRegistry.all()
                val categorized = allCategories.entries
                    .filter { (id, _) -> grouped.containsKey(id) }
                    .sortedBy { it.value.label }

                categorized.forEach { (id, meta) ->
                    val isExpanded = id in expanded
                    PaletteCategoryHeader(meta = meta, expanded = isExpanded) {
                        expanded = if (isExpanded) expanded - id else expanded + id
                    }
                    if (isExpanded) {
                        grouped[id]?.forEach { PaletteNodeItem(spec = it, onAdd = onAddNode) }
                    }
                }
                uncategorized.forEach { PaletteNodeItem(spec = it, onAdd = onAddNode) }
            }
        }
    }
}
