package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.ai.AiTurnStatus
import com.ronjunevaldoz.graphyn.editor.ai.GraphynAiAssistantState
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlinx.coroutines.launch

/** Docked assistant column: transcript of generation turns + a prompt input. */
@Composable
internal fun GraphynAiPanel(
    assistant: GraphynAiAssistantState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.panelBackground)
            .border(1.dp, colors.border)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicText("✨ AI Assistant", style = type.nodeTitle.copy(color = colors.textPrimary))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniButton("Clear") { assistant.clear() }
                MiniButton("✕") { onClose() }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (assistant.turns.isEmpty()) {
                BasicText(
                    "Describe a workflow and I'll build it on the canvas. e.g. \"Fetch a URL and save the body to a file\".",
                    style = type.bodySmall.copy(color = colors.textSecondary),
                )
            }
            assistant.turns.forEach { turn -> AiTurnView(turn) }
        }

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clip(RoundedCornerShape(8.dp)).background(colors.surfaceCard)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(10.dp),
        ) {
            if (prompt.text.isEmpty()) {
                BasicText("Describe a workflow…", style = type.bodySmall.copy(color = colors.textDisabled))
            }
            BasicTextField(
                value = prompt,
                onValueChange = { if (!assistant.generating) prompt = it },
                textStyle = type.bodySmall.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().testTag("ai-panel-prompt"),
            )
        }

        MiniButton(
            label = if (assistant.generating) "Generating…" else "Generate",
            filled = true,
            enabled = !assistant.generating && prompt.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val text = prompt.text
            prompt = TextFieldValue("")
            scope.launch { assistant.submit(text) }
        }
    }
}

@Composable
internal fun MiniButton(
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val interaction = remember { MutableInteractionSource() }
    val bg = if (filled) colors.accent else colors.surfaceCard
    val fg = if (filled) colors.accentForeground else colors.textSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, RoundedCornerShape(6.dp)))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = type.label.copy(color = fg))
    }
}
