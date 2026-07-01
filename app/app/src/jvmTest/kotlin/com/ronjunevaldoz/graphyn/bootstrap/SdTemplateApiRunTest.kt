@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs real SD templates through the execution engine + production SD plugin (HTTP backend → the
 * deployed server-sd, using ~/.graphyn/settings.json for URL + key) — the same call the
 * /execute and /workflows/{id}/run routes make. Opt-in: needs a reachable, configured SD server.
 * Run with: ./gradlew :app:app:jvmTest --tests "*SdTemplateApiRunTest*"
 */
class SdTemplateApiRunTest {

    private val engine: WorkflowExecutionEngine by lazy {
        val plugins = DefaultGraphynPluginRegistry().apply {
            (GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
        }
        WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs)
    }

    private fun run(wf: WorkflowDefinition, outNode: String, outPort: String): String? = runBlocking {
        val started = System.currentTimeMillis()
        val result = engine.execute(wf)
        val elapsed = (System.currentTimeMillis() - started) / 1000
        val path = (result.nodeOutputsByNodeId[outNode]?.get(outPort) as? WorkflowValue.StringValue)?.value
        println("[${wf.name}] ${elapsed}s — statuses=${result.statusByNodeId.values.groupingBy { it }.eachCount()}")
        result.errorsByNodeId.forEach { (id, err) -> println("  ERROR $id: $err") }
        println("  output $outNode.$outPort = $path")
        path
    }

    @Test
    fun fluxTxt2Img() {
        val out = run(fluxTxt2ImgWorkflow, "txt2img", "image")
        assertTrue(out != null && java.io.File(out).isFile, "expected a generated image file, got $out")
    }
}
