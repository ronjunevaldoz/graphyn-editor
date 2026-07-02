package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

internal enum class GraphynRightRailTab { Inspector, StableDiffusion }

@Composable
internal fun GraphynRightRail(
    modifier: Modifier,
    selectedTab: GraphynRightRailTab,
    onSelectTab: (GraphynRightRailTab) -> Unit,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    panels: EditorPanelRegistry,
    validationErrors: List<ValidationError>,
    sdServerControl: SdServerControl?,
    onEnterSubgraph: ((WorkflowDefinition) -> Unit)? = null,
) {
    val colors = GraphynDs.colors
    Box(modifier.fillMaxSize().background(colors.panelBackground).border(1.dp, colors.border)) {
        Box(Modifier.fillMaxSize().padding(top = 70.dp)) {
            when (selectedTab) {
                GraphynRightRailTab.Inspector -> GraphynInspectorPanel(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    nodeSpecs = nodeSpecs,
                    panels = panels,
                    validationErrors = validationErrors,
                    onEnterSubgraph = onEnterSubgraph,
                )
                GraphynRightRailTab.StableDiffusion -> if (sdServerControl != null) {
                    GraphynStableDiffusionPanel(
                        modifier = Modifier.fillMaxSize(),
                        control = sdServerControl,
                    )
                } else EmptyRailState("Stable Diffusion is not configured.")
            }
        }
        GraphynRightRailTrail(
            modifier = Modifier.align(Alignment.TopStart),
            selectedTab = selectedTab,
            onSelectTab = onSelectTab,
            hasSd = sdServerControl != null,
        )
    }
}

@Composable
private fun GraphynRightRailTrail(
    modifier: Modifier = Modifier,
    selectedTab: GraphynRightRailTab,
    onSelectTab: (GraphynRightRailTab) -> Unit,
    hasSd: Boolean,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("RIGHT RAIL", style = type.label.copy(color = colors.textSecondary))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolbarPill("Inspector", selected = selectedTab == GraphynRightRailTab.Inspector) {
                onSelectTab(GraphynRightRailTab.Inspector)
            }
            if (hasSd) ToolbarPill("Stable Diffusion", selected = selectedTab == GraphynRightRailTab.StableDiffusion) {
                onSelectTab(GraphynRightRailTab.StableDiffusion)
            }
        }
    }
}

@Composable
private fun EmptyRailState(message: String) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(message, style = type.bodySmall.copy(color = colors.textDisabled))
    }
}
