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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.ai.GraphynAiAssistantState
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlinx.coroutines.launch

/** Full-screen overlay modal for the AI workflow assistant. Click the scrim or ✕ to dismiss. */
@Composable
internal fun GraphynAiDialog(
    assistant: GraphynAiAssistantState,
    onDismiss: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf(TextFieldValue("")) }
    val shape = RoundedCornerShape(12.dp)

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 480.dp, height = 540.dp)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(colors.panelBackground)
                .border(1.dp, colors.border, shape)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BasicText("✨ AI Assistant", style = type.nodeTitle.copy(color = colors.textPrimary))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniButton("Clear") { assistant.clear() }
                    MiniButton("✕") { onDismiss() }
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (assistant.turns.isEmpty()) {
                    BasicText(
                    "Describe a workflow to build, or ask me to analyze the current one.",
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
                    BasicText("Build or analyze a workflow…", style = type.bodySmall.copy(color = colors.textDisabled))
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = { if (!assistant.generating) prompt = it },
                    textStyle = type.bodySmall.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().testTag("ai-dialog-prompt"),
                )
            }
            MiniButton(
                label = if (assistant.generating) "Thinking…" else "Ask",
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
}
