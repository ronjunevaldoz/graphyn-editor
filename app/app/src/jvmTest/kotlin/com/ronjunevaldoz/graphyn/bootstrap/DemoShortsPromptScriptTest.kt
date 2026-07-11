package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.workflows.*
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoShortsPromptScriptTest {
    @Test
    fun scenePromptFallsBackWhenPromptFieldIsMissing() = runBlocking {
        val registry = DefaultGraphynPluginRegistry().apply { install(ScriptPlugin) }
        val result = registry.nodeExecutors.resolve("script.eval")!!.execute(
            mapOf(
                "code" to WorkflowValue.StringValue(shortsScenePromptScript(1)),
                "input" to WorkflowValue.ListValue(
                    listOf(
                        WorkflowValue.RecordValue(
                            mapOf(
                                "caption" to WorkflowValue.StringValue("Neon alley chase"),
                                "camera_move" to WorkflowValue.StringValue("gentle push-in"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(WorkflowValue.StringValue("Neon alley chase"), result["result"])
    }
}
