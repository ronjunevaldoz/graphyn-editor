package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

internal fun resolveWorkflow(workflowName: String, options: Map<String, String>): WorkflowDefinition = when {
    workflowName.equals(STORYBOARD_WORKFLOW_KEY, ignoreCase = true) -> imageMotionStoryboardShortWorkflow(
        topic = options["topic"] ?: "a quick weeknight pasta dinner",
        width = options["width"]?.toInt() ?: SHORTS_WIDTH,
        height = options["height"]?.toInt() ?: SHORTS_HEIGHT,
        useCharacterSheet = options["character_sheet"]?.toBooleanStrictOrNull() ?: false,
    )
    workflowName.equals(COMPARISON_WORKFLOW_KEY, ignoreCase = true) -> comparisonShortWorkflow(
        topic = options["topic"] ?: "commonly confused everyday concepts",
        width = options["width"]?.toInt() ?: SHORTS_WIDTH,
        height = options["height"]?.toInt() ?: SHORTS_HEIGHT,
        mascotDescription = options["mascot"] ?: com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION,
        useKenBurns = options["ken_burns"]?.toBooleanStrictOrNull() ?: true,
    )
    workflowName.equals(RECAPTION_WORKFLOW_KEY, ignoreCase = true) -> {
        val styleOverrides = options.filterKeys { it in CAPTION_STYLE_DEFAULTS }.mapValues { (key, raw) -> toWorkflowValueLike(CAPTION_STYLE_DEFAULTS.getValue(key), raw) }
        recaptionWorkflow(
            stitchedVideoPath = options["video"] ?: "$STORYBOARD_OUTPUT_BASE.stitched.mp4",
            storyboardJsonPath = options["storyboard"] ?: "$STORYBOARD_OUTPUT_BASE.storyboard.json",
            styleOverrides = styleOverrides,
            outputPath = options["output"] ?: "$STORYBOARD_OUTPUT_BASE.recaptioned.mp4",
            ttsEngine = resolveTtsEngineChoice(options, TtsEngineChoice("qwen3", mapOf("voice" to s("")))),
        )
    }
    workflowName.equals(REGENERATE_SCENE_WORKFLOW_KEY, ignoreCase = true) -> {
        val index = options["index"]?.toIntOrNull() ?: error("Missing index=<0..${STORYBOARD_SCENE_COUNT - 1}> for $REGENERATE_SCENE_WORKFLOW_KEY")
        val storyboardPath = options["storyboard"] ?: "$STORYBOARD_OUTPUT_BASE.storyboard.json"
        val editRequested = options["edit"]?.toBooleanStrictOrNull() ?: false
        val sidecarPath = "$STORYBOARD_OUTPUT_BASE.scene$index.image.txt"
        val sidecarFile = java.io.File(sidecarPath)
        val editMode = editRequested && sidecarFile.exists()
        if (editRequested && !sidecarFile.exists()) println("WARNING: edit=true requested but no saved image path at $sidecarPath — falling back to full regeneration.")
        val editInstruction = options["instruction"]
        require(!editMode || !editInstruction.isNullOrBlank()) { "workflow=regenerate-scene edit=true requires instruction='...' — an edit instruction, not a scene description." }
        regenerateSceneWorkflow(
            sceneIndex = index,
            prompt = options["prompt"] ?: readStoryboardScenePrompt(storyboardPath, index),
            niche = readStoryboardField(storyboardPath, "niche"),
            visualStyle = readStoryboardField(storyboardPath, "visual_style"),
            character = readStoryboardField(storyboardPath, "character"),
            storyboardJsonPath = storyboardPath,
            outputPath = options["output"] ?: "$STORYBOARD_OUTPUT_BASE.mp4",
            ttsEngine = resolveTtsEngineChoice(options, TtsEngineChoice("say", mapOf("voice_id" to s("Samantha"), "speed" to d(1.0)))),
            editMode = editMode,
            editReferenceImagePath = if (editMode) sidecarFile.readText().trim() else null,
            editInstruction = if (editMode) editInstruction else null,
        )
    }
    workflowName.equals(CHARACTER_SAMPLES_WORKFLOW_KEY, ignoreCase = true) -> characterReferenceSamplesWorkflow(
        idEmbedPath = options["id_embed_path"] ?: error("Missing id_embed_path=<path to id_embeds.bin>. $faceDetectInstructions"),
        prompts = options["prompts"]?.split("|")?.map { it.trim() } ?: CharacterSamplePrompts.DEFAULT,
        styleStrength = options["style_strength"]?.toDoubleOrNull() ?: 20.0,
        steps = options["steps"]?.toIntOrNull() ?: 30,
        cfgScale = options["cfg_scale"]?.toDoubleOrNull() ?: 5.0,
        width = options["width"]?.toIntOrNull() ?: 1024,
        height = options["height"]?.toIntOrNull() ?: 1024,
    )
    else -> {
        val base = WorkflowCatalog.entries.firstOrNull { it.name.equals(workflowName, ignoreCase = true) }?.workflow
            ?: error("Unknown workflow '$workflowName'. Available: $STORYBOARD_WORKFLOW_KEY, $RECAPTION_WORKFLOW_KEY, $REGENERATE_SCENE_WORKFLOW_KEY, $CHARACTER_SAMPLES_WORKFLOW_KEY, ${WorkflowCatalog.entries.joinToString { it.name }}")
        applyNodeOverrides(base, options)
    }
}
