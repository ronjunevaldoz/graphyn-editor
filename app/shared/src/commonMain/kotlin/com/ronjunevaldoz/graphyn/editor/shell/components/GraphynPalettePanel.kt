package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

@Composable
internal fun GraphynPalettePanel(
    modifier: Modifier,
    nodeSpecs: NodeSpecRegistry,
    onAddNode: (NodeSpec) -> Unit,
) {
    GraphynChromePanel(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Palette", style = MaterialTheme.typography.titleMedium)
            val specs = nodeSpecs.all()
            if (specs.isEmpty()) {
                Text("No nodes registered yet.")
            } else {
                Column {
                    specs.forEach { spec ->
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            onClick = { onAddNode(spec) },
                        ) {
                            Text(spec.label)
                        }
                    }
                }
            }
        }
    }
}
