package com.ronjunevaldoz.graphyn.pluginapi

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class GraphynPluginTest {
    @Test
    fun installsNodeSpecsAndExecutors() {
        val registry = DefaultGraphynPluginRegistry()

        registry.install(
            object : GraphynPlugin {
                override val metadata = GraphynPluginMetadata(
                    id = "graphyn.test",
                    displayName = "Test",
                    version = "1.0.0",
                )

                override fun register(registrar: GraphynPluginRegistrar) {
                    registrar.registerNodeSpec(
                        NodeSpec(
                            type = "test.echo",
                            label = "Echo",
                            inputs = listOf(
                                PortSpec(name = "value", type = WorkflowType.StringType, required = false),
                            ),
                            outputs = listOf(
                                PortSpec(name = "value", type = WorkflowType.StringType, required = false),
                            ),
                        ),
                    )
                    registrar.registerExecutor("test.echo") { input ->
                        input
                    }
                }
            },
        )

        assertEquals(1, registry.plugins.size)
        assertNotNull(registry.nodeSpecs.resolve("test.echo"))
        assertNotNull(registry.nodeExecutors.resolve("test.echo"))
    }

    @Test
    fun rejectsDuplicatePluginIds() {
        val registry = DefaultGraphynPluginRegistry()
        val plugin = object : GraphynPlugin {
            override val metadata = GraphynPluginMetadata(
                id = "graphyn.duplicate",
                displayName = "Duplicate",
                version = "1.0.0",
            )

            override fun register(registrar: GraphynPluginRegistrar) = Unit
        }

        registry.install(plugin)

        assertFailsWith<IllegalArgumentException> {
            registry.install(plugin)
        }
    }
}
