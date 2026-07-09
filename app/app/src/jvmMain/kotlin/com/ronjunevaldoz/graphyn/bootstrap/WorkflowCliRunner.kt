@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        val options = parseWorkflowCliOptions(args)
        val workflowName = options["workflow"] ?: error("Missing workflow=<name>. Use one of: ${workflowCliTemplates.joinToString { it.key }}, ${WorkflowCatalog.entries.joinToString { it.name }}")
        val workflow = resolveWorkflow(workflowName, options)
        val plugins = DefaultGraphynPluginRegistry().apply { GraphynBootstrap.runtimePlugins(GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) } }
        if (isSchemaMode(args)) return@runBlocking printSchema(workflow, plugins.nodeSpecs)
        val started = System.currentTimeMillis()
        val result = SdArtifactContext.withWorkflow(workflow.id, workflow.name) { WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs).execute(workflow) }
        val elapsed = (System.currentTimeMillis() - started) / 1000
        println("[${workflow.name}] ${elapsed}s — statuses=${result.statusByNodeId.values.groupingBy { it }.eachCount()}")
        result.errorsByNodeId.forEach { (id, err) -> println("  ERROR $id: $err") }
        result.nodeOutputsByNodeId["output"]?.get("file_path")?.let { println("  output=$it") }
    }
}

private fun isSchemaMode(args: Array<String>) = args.any { it.equals(SCHEMA_MODE_KEY, ignoreCase = true) }

private fun parseWorkflowCliOptions(args: Array<String>): Map<String, String> =
    args.filterNot { it.equals(SCHEMA_MODE_KEY, ignoreCase = true) }.associate { arg ->
        val separator = arg.indexOf('=')
        require(separator > 0) { "Expected key=value, got: $arg" }
        arg.substring(0, separator) to arg.substring(separator + 1).trim('\'', '"')
}
