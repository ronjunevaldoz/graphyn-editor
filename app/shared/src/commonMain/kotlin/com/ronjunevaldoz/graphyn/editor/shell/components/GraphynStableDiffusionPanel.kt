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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import com.ronjunevaldoz.graphyn.editor.server.SdServerJobModel
import com.ronjunevaldoz.graphyn.editor.server.SdServerStatusModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GraphynStableDiffusionPanel(
    modifier: Modifier,
    control: SdServerControl,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<SdServerStatusModel?>(null) }
    var jobs by remember { mutableStateOf(emptyList<SdServerJobModel>()) }
    LaunchedEffect(control) {
        while (true) {
            if (control.ping()) {
                status = control.status()
                jobs = control.jobs()
            } else {
                status = null
                jobs = emptyList()
            }
            delay(if (status?.busy == true) 1500 else 4000)
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText("STABLE DIFFUSION", style = GraphynDs.type.panelTitle.copy(color = GraphynDs.colors.textSecondary))
        StatusCard(status = status, onUnload = { scope.launch { status = control.unload() } })
        JobsSection(jobs = jobs, onCancel = { jobId -> scope.launch { control.cancel(jobId); jobs = control.jobs() } })
    }
}

@Composable
private fun StatusCard(status: SdServerStatusModel?, onUnload: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.surfaceCard).border(1.dp, colors.border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText("Server Specs", style = type.label.copy(color = colors.textSecondary))
        if (status == null) {
            BasicText("Waiting for server…", style = type.bodySmall.copy(color = colors.textDisabled))
            return@Column
        }
        val gpu = status.gpus.firstOrNull()
        SpecLine("CPU", if (status.cpuCores > 0) "${status.cpuCores} cores" else "n/a")
        SpecLine("RAM", "${formatGb(status.ramAvailableMb)} free / ${formatGb(status.ramTotalMb)}")
        SpecLine("GPU", gpu?.name ?: "n/a")
        SpecLine("VRAM", gpu?.let { "${formatGb(it.vramUsedMb.toLong())} / ${formatGb(it.vramTotalMb.toLong())}" } ?: "n/a")
        SpecLine("Model", status.loadedModel?.substringAfterLast('/')?.substringAfterLast('\\') ?: "idle")
        SpecLine("Busy", if (status.busy) "yes" else "no")
        if (status.loadedModel != null && !status.busy) MiniButton("Unload", filled = true, modifier = Modifier.fillMaxWidth()) { onUnload() }
    }
}

@Composable
private fun JobsSection(jobs: List<SdServerJobModel>, onCancel: (String) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(colors.surfaceCard).border(1.dp, colors.border, shape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText("Jobs", style = type.label.copy(color = colors.textSecondary))
        if (jobs.isEmpty()) {
            BasicText("No active or queued jobs.", style = type.bodySmall.copy(color = colors.textDisabled))
            return@Column
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { jobs.forEach { job -> JobRow(job = job, onCancel = onCancel) } }
    }
}

@Composable
private fun JobRow(job: SdServerJobModel, onCancel: (String) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(colors.panelBackground)
            .border(1.dp, colors.border, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicText(job.id, style = type.label.copy(color = colors.textPrimary))
            if (job.state == "RUNNING" || job.state == "QUEUED") MiniButton("Cancel") { onCancel(job.id) }
        }
        BasicText(job.state, style = type.bodySmall.copy(color = colors.textSecondary))
        job.error?.takeIf { it.isNotBlank() }?.let {
            BasicText(it, style = type.bodySmall.copy(color = colors.accent))
        }
    }
}

@Composable
private fun SpecLine(label: String, value: String) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BasicText(label, style = type.bodySmall.copy(color = colors.textSecondary))
        BasicText(value, style = type.bodySmall.copy(color = colors.textPrimary))
    }
}

// String.format is JVM-only; round to 1 decimal place manually so this compiles on every target.
private fun formatGb(mb: Long): String = "${kotlin.math.round(mb / 1024.0 * 10) / 10} GB"
