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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.core.store.SettingsStore
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlinx.coroutines.launch

/**
 * Settings/credentials modal: edit the SD server URL + API key and persist them via [store].
 * The in-app way to set credentials, since GUI-launched desktop apps don't inherit shell env vars.
 * Click the scrim or ✕ to dismiss without saving.
 */
@Composable
internal fun GraphynCredentialsDialog(
    store: SettingsStore,
    onDismiss: () -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(12.dp)

    var url by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val s = store.load()
        url = s.sdServerUrl
        apiKey = s.sdApiKey
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center).size(width = 480.dp, height = 300.dp)
                .shadow(16.dp, shape).clip(shape).background(colors.panelBackground)
                .border(1.dp, colors.border, shape).padding(16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                BasicText("Credentials", style = type.nodeTitle.copy(color = colors.textPrimary))
                MiniButton("✕") { onDismiss() }
            }
            Field("SD Server URL", url, "https://…/stablediffusion", "cred-url") { url = it }
            Field("SD API Key", apiKey, "bearer token", "cred-key") { apiKey = it }
            BasicText(
                "Stored in ~/.graphyn/settings.json. Takes precedence over environment variables.",
                style = type.bodySmall.copy(color = colors.textSecondary),
            )
            MiniButton("Save", filled = true, modifier = Modifier.fillMaxWidth()) {
                scope.launch {
                    store.save(GraphynSettings(sdServerUrl = url.trim(), sdApiKey = apiKey.trim()))
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, hint: String, tag: String, onChange: (String) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = type.bodySmall.copy(color = colors.textSecondary))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 36.dp).clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceCard).border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(10.dp),
        ) {
            if (value.isEmpty()) BasicText(hint, style = type.bodySmall.copy(color = colors.textDisabled))
            BasicTextField(
                value = value, onValueChange = onChange,
                textStyle = type.bodySmall.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().testTag(tag),
            )
        }
    }
}
