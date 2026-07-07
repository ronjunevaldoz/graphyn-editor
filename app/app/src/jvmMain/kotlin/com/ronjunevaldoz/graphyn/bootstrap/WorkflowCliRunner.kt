@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private const val STORYBOARD_WORKFLOW_KEY = "storyboard"
private const val RECAPTION_WORKFLOW_KEY = "recaption"
private const val REGENERATE_SCENE_WORKFLOW_KEY = "regenerate-scene"
private const val SCHEMA_MODE_KEY = "schema"

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)

/**
 * Headless runner for any [WorkflowCatalog] entry, plus three storyboard modes that aren't fixed
 * catalog entries since they need CLI args at build time:
 * - `storyboard` — full run from a `topic` (Ollama + Flux + captions + narration).
 * - `recaption` — restyle captions/narration on an already-`storyboard`-run's saved clip, skipping
 *   the expensive Ollama/Flux steps entirely.
 * - `regenerate-scene` — redo one scene's image and re-stitch with the other saved clips.
 *
 * Usage:
 * ```
 * ./gradlew :app:app:runWorkflowCli --args="workflow=storyboard topic='a cozy coffee shop'"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=storyboard topic='...' width=480 height=848"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=storyboard topic='...' character_sheet=true"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=recaption font_size=60 text_color='#FFDD00'"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=recaption tts_engine=say"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=recaption tts_engine=qwen3 reference_audio_path=/path/to/voice.wav"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=regenerate-scene index=1 prompt='a new prompt'"
 * ./gradlew :app:app:runWorkflowCli --args="workflow=regenerate-scene index=1 edit=true instruction='change the shirt to red'"
 * ./gradlew :app:app:runWorkflowCli --args="schema workflow=storyboard"
 * ```
 * `tts_engine=say|qwen3|oute` (optional on `recaption`/`regenerate-scene`) swaps the narration
 * voice/engine without touching Ollama/Flux — see [resolveTtsEngineChoice] for the per-engine
 * option keys (`voice_id`/`speed` for say; `voice`/`reference_audio_path`/`temperature` for
 * qwen3; `language`/`voice`/`instruct`/`temperature`/`seed` for oute).
 *
 * `workflow` matches a [WorkflowCatalog] entry name (case-insensitive) or one of the modes above.
 */
fun main(args: Array<String>) = runBlocking {
    val schemaMode = args.any { it.equals(SCHEMA_MODE_KEY, ignoreCase = true) }
    val options = args.filterNot { it.equals(SCHEMA_MODE_KEY, ignoreCase = true) }.associate { arg ->
        val separator = arg.indexOf('=')
        require(separator > 0) { "Expected key=value, got: $arg" }
        arg.substring(0, separator) to arg.substring(separator + 1).trim('\'', '"')
    }
    val workflowName = options["workflow"]
        ?: error("Missing workflow=<name>. Use 'storyboard' or a WorkflowCatalog entry: ${WorkflowCatalog.entries.joinToString { it.name }}")

    val workflow = resolveWorkflow(workflowName, options)

    val plugins = DefaultGraphynPluginRegistry().apply {
        GraphynBootstrap.runtimePlugins(GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
    }

    if (schemaMode) {
        printSchema(workflow, plugins.nodeSpecs)
        return@runBlocking
    }

    val engine = WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs)
    val started = System.currentTimeMillis()
    val result = SdArtifactContext.withWorkflow(workflow.id, workflow.name) { engine.execute(workflow) }
    val elapsed = (System.currentTimeMillis() - started) / 1000

    println("[${workflow.name}] ${elapsed}s — statuses=${result.statusByNodeId.values.groupingBy { it }.eachCount()}")
    result.errorsByNodeId.forEach { (id, err) -> println("  ERROR $id: $err") }
    result.nodeOutputsByNodeId["output"]?.get("file_path")?.let { println("  output=$it") }
    Unit
}

/**
 * Reads `tts_engine=say|qwen3|oute` plus that engine's own option keys, falling back to
 * [default] (each workflow's current hardcoded voice/engine) when `tts_engine=` is omitted —
 * so existing invocations without it are unaffected.
 */
private fun resolveTtsEngineChoice(options: Map<String, String>, default: TtsEngineChoice): TtsEngineChoice {
    val engine = options["tts_engine"] ?: return default
    val params: Map<String, WorkflowValue> = when (engine) {
        "say" -> mapOf(
            "voice_id" to s(options["voice_id"] ?: "Samantha"),
            "speed" to d(options["speed"]?.toDoubleOrNull() ?: 1.0),
        )
        "qwen3" -> mapOf(
            "voice" to s(options["voice"] ?: ""),
            "reference_audio_path" to s(options["reference_audio_path"] ?: ""),
            "temperature" to d(options["temperature"]?.toDoubleOrNull() ?: 0.1),
        )
        "oute" -> mapOf(
            "language" to s(options["language"] ?: "en"),
            "voice" to s(options["voice"] ?: "default"),
            "instruct" to s(options["instruct"] ?: ""),
            "temperature" to d(options["temperature"]?.toDoubleOrNull() ?: 0.7),
            "seed" to i(options["seed"]?.toIntOrNull() ?: 42),
        )
        else -> error("Unknown tts_engine '$engine'. Use say, qwen3, or oute.")
    }
    return TtsEngineChoice(engine, params)
}

private fun resolveWorkflow(workflowName: String, options: Map<String, String>): WorkflowDefinition = when {
    workflowName.equals(STORYBOARD_WORKFLOW_KEY, ignoreCase = true) ->
        imageMotionStoryboardShortWorkflow(
            topic = options["topic"] ?: "a quick weeknight pasta dinner",
            width = options["width"]?.toInt() ?: SHORTS_WIDTH,
            height = options["height"]?.toInt() ?: SHORTS_HEIGHT,
            useCharacterSheet = options["character_sheet"]?.toBooleanStrictOrNull() ?: false,
        )

    workflowName.equals(RECAPTION_WORKFLOW_KEY, ignoreCase = true) -> {
        val styleOverrides = options.filterKeys { it in CAPTION_STYLE_DEFAULTS }
            .mapValues { (key, raw) -> toWorkflowValueLike(CAPTION_STYLE_DEFAULTS.getValue(key), raw) }
        recaptionWorkflow(
            stitchedVideoPath = options["video"] ?: "$STORYBOARD_OUTPUT_BASE.stitched.mp4",
            storyboardJsonPath = options["storyboard"] ?: "$STORYBOARD_OUTPUT_BASE.storyboard.json",
            styleOverrides = styleOverrides,
            outputPath = options["output"] ?: "$STORYBOARD_OUTPUT_BASE.recaptioned.mp4",
            ttsEngine = resolveTtsEngineChoice(options, TtsEngineChoice("qwen3", mapOf("voice" to s("")))),
        )
    }

    workflowName.equals(REGENERATE_SCENE_WORKFLOW_KEY, ignoreCase = true) -> {
        val index = options["index"]?.toIntOrNull()
            ?: error("Missing index=<0..${STORYBOARD_SCENE_COUNT - 1}> for $REGENERATE_SCENE_WORKFLOW_KEY")
        val storyboardPath = options["storyboard"] ?: "$STORYBOARD_OUTPUT_BASE.storyboard.json"

        val editRequested = options["edit"]?.toBooleanStrictOrNull() ?: false
        val sidecarPath = "$STORYBOARD_OUTPUT_BASE.scene$index.image.txt"
        val sidecarFile = File(sidecarPath)
        val editMode = editRequested && sidecarFile.exists()
        if (editRequested && !sidecarFile.exists()) {
            println("WARNING: edit=true requested but no saved image path at $sidecarPath — falling back to full regeneration.")
        }
        val editInstruction = options["instruction"]
        require(!editMode || !editInstruction.isNullOrBlank()) {
            "workflow=regenerate-scene edit=true requires instruction='...' — an edit instruction, not a scene description."
        }

        regenerateSceneWorkflow(
            sceneIndex = index,
            prompt = options["prompt"] ?: readStoryboardScenePrompt(storyboardPath, index),
            niche = readStoryboardField(storyboardPath, "niche"),
            visualStyle = readStoryboardField(storyboardPath, "visual_style"),
            character = readStoryboardField(storyboardPath, "character"),
            storyboardJsonPath = storyboardPath,
            outputPath = options["output"] ?: "$STORYBOARD_OUTPUT_BASE.mp4",
            ttsEngine = resolveTtsEngineChoice(
                options,
                TtsEngineChoice("say", mapOf("voice_id" to s("Samantha"), "speed" to d(1.0))),
            ),
            editMode = editMode,
            editReferenceImagePath = if (editMode) sidecarFile.readText().trim() else null,
            editInstruction = if (editMode) editInstruction else null,
        )
    }

    else -> {
        val base = WorkflowCatalog.entries.firstOrNull { it.name.equals(workflowName, ignoreCase = true) }?.workflow
            ?: error(
                "Unknown workflow '$workflowName'. Available: $STORYBOARD_WORKFLOW_KEY, $RECAPTION_WORKFLOW_KEY, " +
                    "$REGENERATE_SCENE_WORKFLOW_KEY, ${WorkflowCatalog.entries.joinToString { it.name }}",
            )
        applyNodeOverrides(base, options)
    }
}

/**
 * Applies `nodeId.port=value` CLI options as node config overrides on a fixed [WorkflowCatalog]
 * entry — e.g. `img2img.init_image=/path/to.png img2img.strength=0.6` — without needing a
 * dedicated CLI mode per workflow. Each raw string is converted to match the node's *existing*
 * config value type for that port when one is already set (int/double/bool/string) — sending a
 * bare string for a double-typed port like `strength` would otherwise fail the executor's type
 * check silently and fall back to its default instead of applying the override.
 */
private fun applyNodeOverrides(workflow: WorkflowDefinition, options: Map<String, String>): WorkflowDefinition {
    val overridesByNode = options.entries
        .mapNotNull { (key, value) -> key.indexOf('.').takeIf { it > 0 }?.let { key.substring(0, it) to (key.substring(it + 1) to value) } }
        .groupBy({ it.first }, { it.second })
    if (overridesByNode.isEmpty()) return workflow
    return workflow.copy(
        nodes = workflow.nodes.map { node ->
            overridesByNode[node.id]?.let { ports ->
                node.copy(config = node.config + ports.associate { (port, value) ->
                    val existing = node.config[port]
                    port to (existing?.let { toWorkflowValueLike(it, value) } ?: WorkflowValue.StringValue(value))
                })
            } ?: node
        },
    )
}

private fun toWorkflowValueLike(default: WorkflowValue, raw: String): WorkflowValue = when (default) {
    is WorkflowValue.IntValue -> WorkflowValue.IntValue(raw.toInt())
    is WorkflowValue.DoubleValue -> WorkflowValue.DoubleValue(raw.toDouble())
    is WorkflowValue.ListValue -> WorkflowValue.ListValue(raw.split(',').map { WorkflowValue.StringValue(it.trim()) })
    is WorkflowValue.BooleanValue -> WorkflowValue.BooleanValue(raw.toBooleanStrict())
    else -> WorkflowValue.StringValue(raw)
}

private fun readStoryboardField(storyboardJsonPath: String, field: String): String =
    Json.parseToJsonElement(File(storyboardJsonPath).readText()).jsonObject[field]?.jsonPrimitive?.content.orEmpty()

private fun readStoryboardScenePrompt(storyboardJsonPath: String, index: Int): String {
    val scenes = Json.parseToJsonElement(File(storyboardJsonPath).readText()).jsonObject["scenes"]?.jsonArray
    return scenes?.getOrNull(index)?.jsonObject?.get("prompt")?.jsonPrimitive?.content.orEmpty()
}

/** One overridable input port, addressed by dotted node-id path (subgraphs are joined with `/`). */
private data class WorkflowSchemaEntry(
    val path: String,
    val type: String,
    val required: Boolean,
    val description: String?,
)

/**
 * Walks [workflow] (recursing into nested `subgraph`s) and reports every input port that has no
 * internal [com.ronjunevaldoz.graphyn.core.model.ConnectionRef] targeting it — i.e. every port a
 * caller could set via `NodeRef.config` or a `/workflows/{id}/run` override. Boundary-port
 * free-matching (see [SubgraphDemoPlugin]) means the truly overridable ports often live one or
 * more subgraph levels deeper than the outer node's own declared [NodeSpec], so this recurses
 * rather than only inspecting the top-level nodes.
 */
private fun computeWorkflowSchema(
    workflow: WorkflowDefinition,
    nodeSpecs: NodeSpecRegistry,
    pathPrefix: String = "",
): List<WorkflowSchemaEntry> {
    val connectedTargets = workflow.connections.map { it.toNodeId to it.toPort }.toSet()
    return workflow.nodes.flatMap { node ->
        val ownPorts = nodeSpecs.resolve(node.type)?.inputs.orEmpty()
            .filter { port -> (node.id to port.name) !in connectedTargets }
            .map { port -> WorkflowSchemaEntry("$pathPrefix${node.id}.${port.name}", port.type.toString(), port.required, port.description) }
        val nestedPorts = node.subgraph?.let { computeWorkflowSchema(it, nodeSpecs, "$pathPrefix${node.id}/") }.orEmpty()
        ownPorts + nestedPorts
    }
}

private fun printSchema(workflow: WorkflowDefinition, nodeSpecs: NodeSpecRegistry) {
    println("Schema for '${workflow.name}' — overridable inputs (node-path: type, description):")
    computeWorkflowSchema(workflow, nodeSpecs).forEach { entry ->
        val optional = if (entry.required) "" else " (optional)"
        val hint = entry.description?.let { " — $it" }.orEmpty()
        println("  ${entry.path}: ${entry.type}$optional$hint")
    }
}
