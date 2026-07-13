@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.plugins.gmail.GmailEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.mediaai.MediaAiEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.mediaai.MediaAiPlugin
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCoreEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCorePlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionPlugin

/**
 * JVM-only editor plugins for service integrations.
 */
object GraphynBootstrapJvm {
    val mediaRuntimePlugins: List<GraphynPlugin> = listOf(
        MediaCorePlugin(),
        MediaAiPlugin(),
        ScriptPlugin,
        StableDiffusionPlugin(HttpStableDiffusionBackend()),
    )

    val serviceIntegrationEditorPlugins: List<GraphynEditorPlugin> = listOf(
        GmailEditorPlugin,
        LinkedInEditorPlugin,
        MediaCoreEditorPlugin,
        MediaAiEditorPlugin,
    )
}
