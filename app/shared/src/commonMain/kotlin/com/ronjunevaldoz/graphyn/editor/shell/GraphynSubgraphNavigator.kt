package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState

/** One level in the subgraph navigation stack. */
data class SubgraphFrame(val label: String, val state: GraphynEditorState)

/**
 * Wraps [GraphynEditorShell] with subgraph drill-in navigation and optional home navigation.
 *
 * A breadcrumb bar appears whenever there is a home button ([onHome]) or the user has
 * drilled into a subgraph. Clicking home or a breadcrumb segment pops back to that level.
 *
 * @param onHome Optional callback to return to a launcher or parent screen. When set, a
 *   "⌂" home button is always visible in the navigation bar alongside the workflow name.
 */
@Composable
fun GraphynSubgraphNavigator(
    dependencies: GraphynEditorShellDependencies,
    branding: GraphynBranding = GraphynBranding(),
    appearanceState: GraphynAppearanceState = rememberGraphynAppearanceState(),
    state: GraphynEditorState? = null,
    onHome: (() -> Unit)? = null,
) {
    var stack by remember { mutableStateOf(emptyList<SubgraphFrame>()) }
    val rootState = state ?: rememberGraphynEditorState(nodeSpecs = dependencies.nodeSpecs)
    val activeState = stack.lastOrNull()?.state ?: rootState

    val navDependencies = remember(dependencies) {
        dependencies.copy(
            onEnterSubgraph = { label, inner ->
                val innerState = GraphynEditorState(
                    initialWorkflow = inner,
                    nodeSpecs = dependencies.nodeSpecs,
                )
                stack = stack + SubgraphFrame(label, innerState)
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        GraphynEditorShell(
            dependencies = navDependencies,
            branding = branding,
            appearanceState = appearanceState,
            state = activeState,
        )
        val showNavBar = onHome != null || stack.isNotEmpty()
        if (showNavBar) {
            SubgraphNavigationBar(
                rootLabel = rootState.workflow?.name ?: "Untitled",
                stack = stack,
                onHome = onHome,
                onNavigateTo = { depth -> stack = stack.take(depth) },
                modifier = Modifier.align(Alignment.TopStart).padding(start = 228.dp, top = 12.dp),
            )
        }
    }
}

@Composable
private fun SubgraphNavigationBar(
    rootLabel: String,
    stack: List<SubgraphFrame>,
    onHome: (() -> Unit)?,
    onNavigateTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.panelBackground.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onHome != null) {
            val homeInteraction = remember { MutableInteractionSource() }
            Box(Modifier.clickable(interactionSource = homeInteraction, indication = null, onClick = onHome)) {
                BasicText("⌂", style = type.bodySmall.copy(color = colors.accent))
            }
            BasicText(" · ", style = type.bodySmall.copy(color = colors.textDisabled))
        }
        val rootInteraction = remember { MutableInteractionSource() }
        val rootIsClickable = stack.isNotEmpty()
        Box(
            if (rootIsClickable) Modifier.clickable(interactionSource = rootInteraction, indication = null) { onNavigateTo(0) }
            else Modifier,
        ) {
            BasicText(rootLabel, style = type.bodySmall.copy(color = if (rootIsClickable) colors.accent else colors.textPrimary))
        }
        stack.forEachIndexed { index, frame ->
            BasicText(" › ", style = type.bodySmall.copy(color = colors.textDisabled))
            val isLast = index == stack.size - 1
            if (isLast) {
                BasicText(frame.label, style = type.bodySmall.copy(color = colors.textPrimary))
            } else {
                val crumbInteraction = remember(index) { MutableInteractionSource() }
                Box(Modifier.clickable(interactionSource = crumbInteraction, indication = null) { onNavigateTo(index + 1) }) {
                    BasicText(frame.label, style = type.bodySmall.copy(color = colors.accent))
                }
            }
        }
    }
}
