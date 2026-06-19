package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
 * When [onHome] is provided a "← Home" button appears in the top toolbar. A canvas breadcrumb
 * overlay appears only while the user is drilled into a subgraph, showing the path and letting
 * them tap any segment to pop back.
 *
 * @param onHome Called when the user taps "← Home" in the toolbar; typically sets `openWorkflow = null`
 *   to return to a launcher screen.
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
    // Keyed by inner workflow id so positions survive pop-and-re-enter.
    val stateCache = remember { mutableMapOf<String, GraphynEditorState>() }

    val navDependencies = remember(dependencies, stack.isNotEmpty()) {
        dependencies.copy(
            onHome = onHome,
            onEnterSubgraph = { label, inner ->
                val innerState = stateCache.getOrPut(inner.id) {
                    GraphynEditorState(initialWorkflow = inner, nodeSpecs = dependencies.nodeSpecs)
                }
                stack = stack + SubgraphFrame(label, innerState)
            },
            onExitSubgraph = if (stack.isNotEmpty()) ({ stack = stack.dropLast(1) }) else null,
            // Canvas breadcrumb only appears when drilled into a subgraph; home lives in the toolbar.
            canvasTopStart = if (stack.isNotEmpty()) ({
                SubgraphNavigationBar(
                    rootLabel = rootState.workflow?.name ?: "Untitled",
                    stack = stack,
                    onNavigateTo = { depth -> stack = stack.take(depth) },
                )
            }) else null,
        )
    }

    GraphynEditorShell(
        dependencies = navDependencies,
        branding = branding,
        appearanceState = appearanceState,
        state = activeState,
    )
}

@Composable
private fun SubgraphNavigationBar(
    rootLabel: String,
    stack: List<SubgraphFrame>,
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
        val rootInteraction = remember { MutableInteractionSource() }
        Box(Modifier.clickable(interactionSource = rootInteraction, indication = null) { onNavigateTo(0) }) {
            BasicText(rootLabel, style = type.bodySmall.copy(color = colors.accent))
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
