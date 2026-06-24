@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.gmail.GmailEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInEditorPlugin

/**
 * JVM-only editor plugins for service integrations.
 */
object GraphynBootstrapJvm {
    val serviceIntegrationEditorPlugins: List<GraphynEditorPlugin> = listOf(
        GmailEditorPlugin, LinkedInEditorPlugin,
    )
}
