package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynShortcutsPanel
import kotlin.test.Test

class ShortcutsPanelUiTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun panelListsAllActions() = runDesktopComposeUiTest {
        setContent {
            GraphynTheme {
                Box(Modifier.padding(16.dp)) {
                    GraphynShortcutsPanel(shortcutState = GraphynShortcutState())
                }
            }
        }
        onNodeWithText("Keyboard Shortcuts").assertIsDisplayed()
        onNodeWithText("Undo").assertIsDisplayed()
        onNodeWithText("Collapse to Subgraph").assertIsDisplayed()
        onNodeWithText("Reset all").assertIsDisplayed()
    }
}
