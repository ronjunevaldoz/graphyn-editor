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

    private val initImage = "/tmp/aitest/flux.png" // a real local image, uploaded for img2img/img2vid

    private fun WorkflowDefinition.withInit(nodeId: String, path: String) = copy(
        nodes = nodes.map {
            if (it.id == nodeId) it.copy(config = it.config + ("init_image" to WorkflowValue.StringValue(path))) else it
        },
    )

    private fun run(wf: WorkflowDefinition, outNode: String, outPort: String, assertOk: Boolean = true): String? = runBlocking {
        val started = System.currentTimeMillis()
        val result = com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
            .withWorkflow(wf.id, wf.name) { engine.execute(wf) }
        val elapsed = (System.currentTimeMillis() - started) / 1000
        val raw = result.nodeOutputsByNodeId[outNode]?.get(outPort)
        val path = (raw as? WorkflowValue.StringValue)?.value
            ?: ((raw as? WorkflowValue.ListValue)?.items?.firstOrNull() as? WorkflowValue.StringValue)?.value
        println("[${wf.name}] ${elapsed}s — statuses=${result.statusByNodeId.values.groupingBy { it }.eachCount()} — out=$path")
        result.errorsByNodeId.forEach { (id, err) -> println("  ERROR $id: $err") }
        if (assertOk) assertTrue(path != null && java.io.File(path).isFile, "expected an output file, got $path")
        path
    }

    @Test fun fluxTxt2Img() = run(fluxTxt2ImgWorkflow, "txt2img", "image").let {}
    @Test fun qwenTxt2Img() = run(qwenTxt2ImgWorkflow, "txt2img", "image").let {}
    @Test fun qwenImg2Img() = run(qwenImg2ImgWorkflow.withInit("img2img", initImage), "img2img", "image").let {}
    @Test fun wan5bImg2Vid() = run(wan5bImg2VidWorkflow.withInit("img2vid", initImage), "img2vid", "frames").let {}

    /** A14B (~25.8 GB) can't fit a 12 GB card — run for the record, don't fail the suite on OOM. */
    @Test fun wanA14bImg2Vid() = run(wanImg2VidWorkflow.withInit("img2vid", initImage), "img2vid", "frames", assertOk = false).let {}
}
