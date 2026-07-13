package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.plugins.mediaai.MediaAiEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.mediaai.MediaAiPlugin
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCoreEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCorePlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import kotlin.test.Test
import kotlin.test.assertTrue

class MediaPluginsBootstrapTest {
    @Test
    fun desktopBootstrapIncludesMediaRuntimeAndEditorPlugins() {
        assertTrue(GraphynBootstrapJvm.mediaRuntimePlugins.any { it is MediaCorePlugin })
        assertTrue(GraphynBootstrapJvm.mediaRuntimePlugins.any { it is MediaAiPlugin })
        assertTrue(GraphynBootstrapJvm.mediaRuntimePlugins.any { it is ScriptPlugin })
        assertTrue(GraphynBootstrapJvm.serviceIntegrationEditorPlugins.contains(MediaCoreEditorPlugin))
        assertTrue(GraphynBootstrapJvm.serviceIntegrationEditorPlugins.contains(MediaAiEditorPlugin))
    }
}
