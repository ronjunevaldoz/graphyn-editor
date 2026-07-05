package com.ronjunevaldoz.graphyn.plugins.shorts

/**
 * Builds the Ollama prompt that asks a small local model to emit a strict JSON storyboard for
 * [topic]. The shape is validated (and salvaged/fallen-back) downstream by
 * [storyboardValidateExecutor], so this only has to describe the target contract clearly.
 */
internal fun buildStoryboardPrompt(topic: String) = listOf(
    "Write JSON only for a short-form vertical video storyboard about: $topic.",
    "Return exactly this shape:",
    "{",
    "  \"niche\": string,",
    "  \"visual_style\": string,",
    "  \"character\": string,",
    "  \"narration\": string,",
    "  \"scenes\": [ { \"prompt\": string, \"caption\": string } ]",
    "}",
    "Make exactly $STORYBOARD_SCENE_COUNT scenes.",
    "narration is the full spoken voiceover for the whole video, written as one flowing script.",
    "Each scene's caption is a short on-screen text (under 8 words), and prompt is a concrete visual",
    "description for a text-to-image model — specific subject, setting, lighting, no camera jargon.",
    "visual_style is a rendering style that applies to every scene — pick whichever best fits the",
    "topic instead of defaulting to photography, e.g. \"2D anime cel-shaded\", \"Pixar-style 3D cartoon\",",
    "\"flat vector illustration\", \"claymation stop-motion\", \"watercolor painting\", \"pixel art\", or",
    "\"warm cinematic photography, shallow depth of field\" for realistic subjects.",
    "character is a short, reusable visual description of the main subject (appearance, clothing,",
    "distinguishing features — no name needed) so it can be repeated identically in every scene's",
    "prompt for visual continuity. Leave it \"\" if no recurring subject fits the topic.",
).joinToString("\n")
