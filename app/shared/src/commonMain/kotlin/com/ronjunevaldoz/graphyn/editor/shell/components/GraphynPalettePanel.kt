package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun GraphynPalettePanel(
    modifier: Modifier,
    nodeSpecs: NodeSpecRegistry,
    onAddNode: (NodeSpec) -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    var query by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panelBackground)
            .border(width = 1.dp, color = colors.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            BasicText(
                text = "NODES",
                style = type.panelTitle.copy(color = colors.textSecondary),
            )
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
                textStyle = type.bodySmall.copy(color = colors.textPrimary),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        BasicText("Search nodes…", style = type.bodySmall.copy(color = colors.textDisabled))
                    }
                    inner()
                },
            )
        }
        val specs = nodeSpecs.all()
            .let { all -> if (query.isBlank()) all else all.filter { it.label.contains(query, ignoreCase = true) } }
        if (specs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "No nodes\nregistered yet.",
                    style = type.bodySmall.copy(color = colors.textDisabled),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
            ) {
                specs.forEach { spec -> PaletteNodeItem(spec = spec, onAdd = onAddNode) }
            }
        }
    }
}

@Composable
private fun PaletteNodeItem(spec: NodeSpec, onAdd: (NodeSpec) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onAdd(spec) },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.accent),
        )
        BasicText(
            text = spec.label,
            style = type.body.copy(color = colors.textPrimary),
        )
    }
}
