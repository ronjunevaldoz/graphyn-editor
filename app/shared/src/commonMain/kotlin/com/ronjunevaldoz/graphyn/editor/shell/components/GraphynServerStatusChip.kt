package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.server.SdServerControl
import com.ronjunevaldoz.graphyn.editor.server.SdServerStatusModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compact toolbar chip showing the SD server's GPU VRAM + utilization, the loaded model, and busy
 * state; polls [control] every few seconds. The ⏏ button frees the loaded model's VRAM. Renders
 * nothing until the first successful poll (so an unreachable server just stays hidden).
 */
@Composable
internal fun GraphynServerStatusChip(control: SdServerControl) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<SdServerStatusModel?>(null) }
    LaunchedEffect(control) {
        while (true) {
            status = if (control.ping()) control.status() else null
            delay(if (status?.busy == true) 1500 else 4000)
        }
    }
    val s = status ?: return
    val gpu = s.gpus.firstOrNull()
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = Modifier.clip(shape).background(colors.panelBackground).border(1.dp, colors.border, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        gpu?.let {
            val accent = if (it.vramUsedMb > it.vramTotalMb * 0.9) colors.accent else colors.textSecondary
            val usedGb = (it.vramUsedMb * 10 / 1024) / 10.0
            BasicText("⛶ $usedGb/${it.vramTotalMb / 1024}GB · ${it.utilizationPct}%",
                style = type.label.copy(color = accent))
        }
        BasicText(s.loadedModel?.substringAfterLast('/')?.substringAfterLast('\\') ?: "idle",
            style = type.label.copy(color = if (s.loadedModel != null) colors.textPrimary else colors.textDisabled))
        if (s.busy) BasicText("● busy", style = type.label.copy(color = colors.accent))
        if (s.loadedModel != null && !s.busy) {
            MiniButton("⏏") { scope.launch { control.unload()?.let { status = it } } }
        }
    }
}
