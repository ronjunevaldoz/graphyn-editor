package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
internal fun FieldRow(
    name: String? = null,
    description: String? = null,
    hasValue: Boolean = true,
    leading: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val theme = LocalFieldNodeTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        name?.let {
            BasicText(it, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
        }
        if (hasValue) {
            Spacer(Modifier.weight(1f))
        }
        content()
    }
}
