@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.store.InMemoryWorkflowStore
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.gmail.GmailPlugin
import com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInPlugin
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ServiceIntegrationPluginsTest {

    private val roborazziOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun verifyGmailAndLinkedInPluginsAreRegistered() = runDesktopComposeUiTest(width = 1920, height = 1080) {
        val runtimePlugins = GraphynBootstrap.runtimePlugins(extraPlugins = listOf(GmailPlugin, LinkedInPlugin))

        // Verify plugins are loaded
        val gmailPluginLoaded = runtimePlugins.any { it is com.ronjunevaldoz.graphyn.plugins.gmail.GmailPlugin }
        val linkedInPluginLoaded = runtimePlugins.any { it is com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInPlugin }

        assertTrue(gmailPluginLoaded, "Gmail plugin not found in runtime plugins")
        assertTrue(linkedInPluginLoaded, "LinkedIn plugin not found in runtime plugins")

        val editorRegistry = DefaultGraphynEditorPluginRegistry().apply { installAll(GraphynBootstrap.editorPlugins()) }
        val pluginRegistry = DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) }

        val state = GraphynEditorState(WorkflowCatalog.Script.workflow)

        setContent {
            GraphynTheme {
                GraphynEditorShell(
                    dependencies = GraphynEditorShellDependencies(
                        nodeSpecs = pluginRegistry.nodeSpecs,
                        canvasCards = editorRegistry.canvasCards,
                    ),
                    state = state,
                    appearanceState = rememberGraphynAppearanceState(),
                )
            }
        }

        runBlocking {
            snapshotFlow { state.canvasSize.width > 0 && state.canvasSize.height > 0 }
                .first { it }
        }

        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }
}
