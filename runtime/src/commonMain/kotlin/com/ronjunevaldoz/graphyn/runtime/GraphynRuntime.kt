package com.ronjunevaldoz.graphyn.runtime

import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.plugins.control.ControlEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.control.ControlPlugin
import com.ronjunevaldoz.graphyn.plugins.io.IoEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.io.IoPlugin
import com.ronjunevaldoz.graphyn.plugins.json.JsonEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.json.JsonPlugin
import com.ronjunevaldoz.graphyn.plugins.listops.ListOpsEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.listops.ListOpsPlugin
import com.ronjunevaldoz.graphyn.plugins.preview.PreviewEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.preview.PreviewPlugin
import com.ronjunevaldoz.graphyn.plugins.text.TextEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.text.TextPlugin
import com.ronjunevaldoz.graphyn.plugins.types.TypesEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.types.TypesPlugin

/**
 * The production plugin set: the single source of truth for which node types exist in a
 * real Graphyn deployment. Every host — the server, the desktop/web/Android apps — assembles
 * its registries from here so they agree on available nodes and executors.
 *
 * Each entry has a real executor and models a genuine operation (data fetch, transform,
 * control flow, serialization). Sample and demonstration plugins are NOT included here;
 * hosts that want them (e.g. the demo app) add them on top of these lists.
 */
object GraphynRuntime {
    /** Runtime plugins (node specs + executors). Safe for headless hosts — no UI dependency required. */
    val runtimePlugins: List<GraphynPlugin> = listOf(
        ControlPlugin, ListOpsPlugin, TypesPlugin, TextPlugin, IoPlugin, JsonPlugin, PreviewPlugin,
    )

    /** Editor plugins (canvas cards + categories) paired one-to-one with [runtimePlugins]. */
    val editorPlugins: List<GraphynEditorPlugin> = listOf(
        ControlEditorPlugin, ListOpsEditorPlugin, TypesEditorPlugin, TextEditorPlugin,
        IoEditorPlugin, JsonEditorPlugin, PreviewEditorPlugin,
    )
}
