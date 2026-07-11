package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.stringValue as s
import com.ronjunevaldoz.graphyn.workflows.*

internal data class WorkflowCliTemplate(
    val key: String,
    val build: (Map<String, String>) -> WorkflowDefinition,
)

internal val workflowCliTemplates: List<WorkflowCliTemplate> = listOf(
    WorkflowCliTemplate(STORYBOARD_WORKFLOW_KEY) { options ->
        imageMotionStoryboardShortWorkflow(
            topic = options["topic"] ?: "a quick weeknight pasta dinner",
            width = options["width"]?.toInt(),
            height = options["height"]?.toInt(),
            useCharacterSheet = options["character_sheet"]?.toBooleanStrictOrNull(),
        )
    },
    WorkflowCliTemplate(COMPARISON_WORKFLOW_KEY) { options ->
        comparisonShortWorkflow(
            topic = options["topic"] ?: "commonly confused everyday concepts",
            width = options["width"]?.toInt(),
            height = options["height"]?.toInt(),
            mascotDescription = options["mascot"],
            useKenBurns = options["ken_burns"]?.toBooleanStrictOrNull(),
        )
    },
    WorkflowCliTemplate(MASCOT_PREVIEW_WORKFLOW_KEY) { options ->
        mascotPreviewWorkflow(
            mascotDescription = options["mascot"],
            width = options["width"]?.toInt(),
            height = options["height"]?.toInt(),
        )
    },
    WorkflowCliTemplate(MASCOT_PREVIEW_QWEN_WORKFLOW_KEY) { options ->
        mascotPreviewQwenWorkflow(
            mascotDescription = options["mascot"],
            width = options["width"]?.toInt(),
            height = options["height"]?.toInt(),
        )
    },
    WorkflowCliTemplate(IMAGE_EDIT_WORKFLOW_KEY) { options ->
        referenceImageEditWorkflow(
            imagePath = options["image"] ?: error("Missing image=<path to reference image>"),
            instruction = options["instruction"] ?: error("Missing instruction=<edit description>"),
            model = options["model"] ?: "flux-kontext",
            width = options["width"]?.toInt(),
            height = options["height"]?.toInt(),
            steps = options["steps"]?.toInt(),
            cfgScale = options["cfg"]?.toDouble(),
            useLightningLora = options["lightning"]?.toBooleanStrictOrNull(),
            seed = options["seed"]?.toInt(),
            negativePrompt = options["negative"],
        )
    },
    WorkflowCliTemplate(CHARACTER_BASE_WORKFLOW_KEY) { options ->
        characterBaseWorkflow(
            description = options["description"] ?: "a character, plain white background, front view facing directly toward the camera, neutral standing pose, arms relaxed at sides, no text, whole body",
            width = options["width"]?.toInt(),
            height = options["height"]?.toInt(),
            useLlmPromptEnhance = options["enhance"]?.toBooleanStrictOrNull(),
        )
    },
    WorkflowCliTemplate(RECAPTION_WORKFLOW_KEY) { options ->
        val styleOverrides = options.filterKeys { it in CAPTION_STYLE_DEFAULTS }
            .mapValues { (key, raw) -> toWorkflowValueLike(CAPTION_STYLE_DEFAULTS.getValue(key), raw) }
        recaptionWorkflow(
            stitchedVideoPath = options["video"] ?: "$STORYBOARD_OUTPUT_BASE.stitched.mp4",
            storyboardJsonPath = options["storyboard"] ?: "$STORYBOARD_OUTPUT_BASE.storyboard.json",
            styleOverrides = styleOverrides,
            outputPath = options["output"],
            // Matches recaptionWorkflow's own default voice ("Ryan") — this used to fall back to
            // an empty voice string here, silently diverging from the function's real default.
            ttsEngine = resolveTtsEngineChoice(options, TtsEngineChoice("qwen3", mapOf("voice" to s("Ryan")))),
        )
    },
    WorkflowCliTemplate(REGENERATE_SCENE_WORKFLOW_KEY) { options ->
        val index = options["index"]?.toIntOrNull()
            ?: error("Missing index=<0..${STORYBOARD_SCENE_COUNT - 1}> for $REGENERATE_SCENE_WORKFLOW_KEY")
        val storyboardPath = options["storyboard"] ?: "$STORYBOARD_OUTPUT_BASE.storyboard.json"
        val editRequested = options["edit"]?.toBooleanStrictOrNull() ?: false
        val sidecarPath = "$STORYBOARD_OUTPUT_BASE.scene$index.image.txt"
        val sidecarFile = java.io.File(sidecarPath)
        val editMode = editRequested && sidecarFile.exists()
        if (editRequested && !sidecarFile.exists()) println("WARNING: edit=true requested but no saved image path at $sidecarPath — falling back to full regeneration.")
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
            outputPath = options["output"],
            ttsEngine = resolveTtsEngineChoice(options, TtsEngineChoice("say", mapOf("voice_id" to s("Samantha"), "speed" to d(1.0)))),
            editMode = editMode,
            editReferenceImagePath = if (editMode) sidecarFile.readText().trim() else null,
            editInstruction = if (editMode) editInstruction else null,
        )
    },
    WorkflowCliTemplate(CHARACTER_SAMPLES_WORKFLOW_KEY) { options ->
        characterReferenceSamplesWorkflow(
            idEmbedPath = options["id_embed_path"] ?: error("Missing id_embed_path=<path to id_embeds.bin>. $faceDetectInstructions"),
            prompts = options["prompts"]?.split("|")?.map { it.trim() } ?: CharacterSamplePrompts.DEFAULT,
            styleStrength = options["style_strength"]?.toDoubleOrNull(),
            steps = options["steps"]?.toIntOrNull(),
            cfgScale = options["cfg_scale"]?.toDoubleOrNull(),
            width = options["width"]?.toIntOrNull(),
            height = options["height"]?.toIntOrNull(),
        )
    },
)

internal fun resolveWorkflow(workflowName: String, options: Map<String, String>): WorkflowDefinition {
    workflowCliTemplates.firstOrNull { it.key.equals(workflowName, ignoreCase = true) }?.let { return it.build(options) }
    WorkflowCatalog.entries.firstOrNull { it.name.equals(workflowName, ignoreCase = true) }?.let { return applyNodeOverrides(it.workflow, options) }
    error("Unknown workflow '$workflowName'. Available: ${workflowCliTemplates.joinToString { it.key }}, ${WorkflowCatalog.entries.joinToString { it.name }}")
}
