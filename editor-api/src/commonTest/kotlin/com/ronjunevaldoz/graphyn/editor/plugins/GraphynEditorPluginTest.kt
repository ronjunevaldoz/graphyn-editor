@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor.plugins

import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class GraphynEditorPluginTest {
    @Test
    fun installsEditorPanels() {
        val registry = DefaultGraphynEditorPluginRegistry()

        registry.install(
            object : GraphynEditorPlugin {
                override val metadata = GraphynEditorPluginMetadata(
                    id = "graphyn.sample.editor",
                    displayName = "Sample Editor",
                    version = "1.0.0",
                )

                override fun register(registrar: GraphynEditorPluginRegistrar) {
                    registrar.registerPanel(
                        "sample.logger",
                        EditorPanelFactory { _: EditorPanelContext -> }
                    )
                }
            },
        )

        assertNotNull(registry.panels.resolve("sample.logger"))
    }

    @Test
    fun rejectsDuplicateEditorPluginIds() {
        val registry = DefaultGraphynEditorPluginRegistry()
        val plugin = object : GraphynEditorPlugin {
            override val metadata = GraphynEditorPluginMetadata(
                id = "graphyn.duplicate.editor",
                displayName = "Duplicate Editor",
                version = "1.0.0",
            )

            override fun register(registrar: GraphynEditorPluginRegistrar) = Unit
        }

        registry.install(plugin)

        assertFailsWith<IllegalArgumentException> {
            registry.install(plugin)
        }
    }
}
