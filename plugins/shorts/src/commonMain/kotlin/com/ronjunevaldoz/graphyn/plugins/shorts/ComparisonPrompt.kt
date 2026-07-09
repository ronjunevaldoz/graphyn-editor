package com.ronjunevaldoz.graphyn.plugins.shorts

/**
 * Builds the Ollama prompt that asks a small local model to emit a strict JSON comparison-arc
 * ("X vs Y — what's the difference?") for [topic]. Sibling to [buildStoryboardPrompt], not a
 * modification of it — the requested shape is entirely different (paired comparisons, not a
 * flat narrative scene list). Validated (and salvaged/fallen-back) downstream by
 * [comparisonValidateExecutor].
 *
 * Deliberately does NOT request a standalone "narration" field. It used to, but a real generated
 * response wrote flowing narration prose covering 7 distinct comparisons while the structured
 * `pairs` array only captured 4 (one of them blank) — the model's free-form narration had no
 * reliable correspondence to the pairs it actually structured, so the audio and the on-screen
 * comparisons drifted apart. [comparisonValidateExecutor] now builds narration deterministically
 * from each validated pair's own question/answer instead, which can only ever match what's on
 * screen. Asking for one fewer field also gives the model more of its response budget for getting
 * the pairs themselves right.
 */
internal fun buildComparisonPrompt(topic: String) = listOf(
    "Write JSON only for a short-form vertical video comparing pairs of things, about: $topic.",
    "Return exactly this shape:",
    "{",
    "  \"niche\": string,",
    "  \"visual_style\": string,",
    "  \"pairs\": [ { \"label_a\": string, \"label_b\": string, \"prompt_a\": string,",
    "                \"prompt_b\": string, \"question\": string, \"answer\": string } ]",
    "}",
    "Make exactly $COMPARISON_PAIR_COUNT pairs.",
    "Each pair is two things people commonly confuse or want compared. label_a/label_b are short",
    "names (1-3 words). question is a short on-screen caption posing the comparison (under 8",
    "words, e.g. \"What's the difference?\"). answer is a short on-screen caption stating the key",
    "difference (under 10 words).",
    "prompt_a and prompt_b are concrete visual descriptions for a text-to-image model, one per",
    "thing being compared — specific subject, setting, lighting, no camera jargon, no real named",
    "people. Describe an original, generic representative scene for that thing instead.",
    "visual_style is a rendering style that applies to every pair's images — pick whichever best",
    "fits the topic instead of defaulting to photography, e.g. \"2D anime cel-shaded\",",
    "\"Pixar-style 3D cartoon\", \"flat vector illustration\", \"claymation stop-motion\",",
    "\"watercolor painting\", \"pixel art\", or \"warm cinematic photography, shallow depth of",
    "field\" for realistic subjects.",
).joinToString("\n")
