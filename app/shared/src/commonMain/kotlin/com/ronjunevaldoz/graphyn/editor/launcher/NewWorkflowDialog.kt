package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerationResult
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlinx.coroutines.launch

/**
 * Dialog shown when creating a new workflow: describe it in natural language and let an LLM
 * generate the graph, or start blank. [generator] runs against the configured host (or a
 * placeholder offline); [catalog] constrains generation to real node types.
 *
 * On success [onCreate] receives the workflow (generated or blank). Failures stay in the dialog
 * with a retryable message so the user never loses their prompt.
 */
@Composable
fun NewWorkflowDialog(
    generator: WorkflowGenerator,
    catalog: List<NodeSpec>,
    onCreate: (WorkflowDefinition) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf(TextFieldValue("")) }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!generating) onDismiss() }) {
        Column(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceCard)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.foundation.text.BasicText("New Workflow", style = type.appTitle.copy(color = colors.textPrimary))
            androidx.compose.foundation.text.BasicText(
                "Describe what you want, and AI will draft the graph — or start from an empty canvas.",
                style = type.bodySmall.copy(color = colors.textSecondary),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.panelBackground)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                if (prompt.text.isEmpty()) {
                    androidx.compose.foundation.text.BasicText(
                        "e.g. Fetch a webpage and save its body to a file",
                        style = type.bodySmall.copy(color = colors.textDisabled),
                    )
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = { if (!generating) prompt = it },
                    textStyle = type.bodySmall.copy(color = colors.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().testTag("new-workflow-prompt"),
                )
            }

            error?.let {
                androidx.compose.foundation.text.BasicText(it, style = type.bodySmall.copy(color = colors.danger))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogButton("Start blank", filled = false, enabled = !generating) {
                    onCreate(WorkflowDefinition(id = blankId(), name = "Untitled", nodes = emptyList(), connections = emptyList()))
                }
                DialogButton(
                    label = if (generating) "Generating…" else "Generate",
                    filled = true,
                    enabled = !generating && prompt.text.isNotBlank(),
                ) {
                    error = null
                    generating = true
                    scope.launch {
                        when (val result = generator.generate(prompt.text, catalog)) {
                            is WorkflowGenerationResult.Success -> onCreate(result.workflow)
                            is WorkflowGenerationResult.Failure -> { error = result.message; generating = false }
                        }
                    }
                }
            }
        }
    }
}

private fun blankId(): String = "wf-${kotlin.random.Random.nextLong().and(0xFFFFFFFFL)}"

@Composable
private fun DialogButton(label: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interaction = remember { MutableInteractionSource() }
    val bg = if (filled) colors.accent else colors.panelBackground
    val fg = if (filled) colors.accentForeground else colors.textSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, RoundedCornerShape(6.dp)))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.text.BasicText(label, style = type.label.copy(color = fg))
    }
}
