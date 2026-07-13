package com.ronjunevaldoz.graphyn.plugins.shorts

/**
 * Node-type identifiers owned by this plugin. The subgraph-wrapper types (`scene`, `batch`,
 * `storyboard`) expose a single terminal output of a nested [com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition]
 * as one node; the `demo.storyboard.*` types are the compiled Ollama/storyboard executors.
 */
public object ShortsNodeTypes {
    /** Scene subgraph wrapper — runs one reusable shorts scene, exposes its rendered `video`. */
    public const val SCENE_SUBGRAPH: String = "demo.subgraph.scene"

    /** Batch-stitch subgraph wrapper — stitches a small batch of clips into one `video`. */
    public const val BATCH_SUBGRAPH: String = "demo.subgraph.batch"

    /** Storyboard subgraph wrapper — runs the Ollama generator, exposes the validated `value`. */
    public const val STORYBOARD_SUBGRAPH: String = "demo.subgraph.storyboard"

    /** Ollama `/api/generate` URL builder. */
    public const val OLLAMA_URL: String = "demo.storyboard.ollama_url"

    /** Fetch+parse subgraph wrapper — runs the Ollama request/outer-parse/response-path/inner-parse
     * chain shared by [storyboardGeneratorSubgraph] and [comparisonGeneratorSubgraph], exposing just
     * the parsed `value` and a bundled `diagnostics` summary instead of every stage's raw ok/error
     * ports. See [OLLAMA_CHAIN_DIAGNOSTICS]. */
    public const val OLLAMA_FETCH_SUBGRAPH: String = "demo.subgraph.ollama_fetch"

    /** Bundles the Ollama request/parse chain's per-stage ok/error signals into one `diagnostics`
     * string via [ollamaChainDiagnostics] — the sole internal consumer of those raw ports, so a
     * generator subgraph's validate node only needs one wire instead of one per stage. */
    public const val OLLAMA_CHAIN_DIAGNOSTICS: String = "demo.ollama.chain_diagnostics"

    /** Ollama `/api/generate` request-body builder for the storyboard prompt. */
    public const val OLLAMA_BODY: String = "demo.storyboard.ollama_body"

    /** Extracts one top-level string field from a validated storyboard record. */
    public const val STORYBOARD_FIELD: String = "demo.storyboard.field"

    /** Extracts one field of one scene (by index) from a validated storyboard record. */
    public const val STORYBOARD_SCENE_FIELD: String = "demo.storyboard.scene_field"

    /** Builds caption records (text/start_ms/end_ms) from a validated storyboard's scenes. */
    public const val STORYBOARD_CAPTIONS: String = "demo.storyboard.captions"

    /** Validates the Ollama storyboard JSON and unloads the Ollama model (compiled, not scripted). */
    public const val STORYBOARD_VALIDATE: String = "demo.storyboard.validate"

    /** Standalone Ollama-unload gate for pipelines that don't run [STORYBOARD_VALIDATE]. */
    public const val OLLAMA_UNLOAD: String = "demo.ollama.unload"

    /**
     * Standalone bare Ollama text-generation call: prompt in, raw model text out. The self-contained
     * equivalent of Studio's `studio.generate-script`; unlike [STORYBOARD_SUBGRAPH] it does not bundle
     * URL/body-building or JSON validation — it just calls the LLM and returns its text.
     */
    public const val OLLAMA_GENERATE: String = "ollama.generate"

    /** Ollama `/api/generate` request-body builder for the comparison-arc prompt. Sibling to
     * [OLLAMA_BODY], not a shared node — the JSON schema requested is entirely different. */
    public const val COMPARISON_OLLAMA_BODY: String = "demo.comparison.ollama_body"

    /** Validates the Ollama comparison-arc JSON and unloads the Ollama model. Sibling to
     * [STORYBOARD_VALIDATE] with the same salvage/fallback discipline, different shape. */
    public const val COMPARISON_VALIDATE: String = "demo.comparison.validate"

    /** Extracts niche/visual_style/narration from a validated comparison-arc record in one step. */
    public const val COMPARISON_FIELDS: String = "demo.comparison.fields"

    /** Extracts label_a/label_b/prompt_a/prompt_b of one pair (by index) from a validated
     * comparison-arc record in one step. */
    public const val COMPARISON_PAIR_FIELDS: String = "demo.comparison.pair_fields"

    /** Builds caption records (text/start_ms/end_ms) from a validated comparison-arc's pairs —
     * each pair contributes a question beat then an answer beat. */
    public const val COMPARISON_CAPTIONS: String = "demo.comparison.captions"

    /** Divides a measured narration duration evenly across the comparison pairs, with a sane floor —
     * see [comparisonPairDurationExecutor] for why this exists instead of a fixed guess. */
    public const val COMPARISON_PAIR_DURATION: String = "demo.comparison.pair_duration"

    /** Appends a generation timestamp to the validated comparison record before serialization. */
    public const val COMPARISON_METADATA: String = "demo.comparison.metadata"

    /** LLM-based prompt enhancer (Ollama) — expands a rough description into a detailed generation
     * prompt, falling back to the raw prompt unchanged on any failure. Opt-in alternative to the
     * deterministic `media.prompt_enhance` (plain string-joining, no LLM call). */
    public const val PROMPT_ENHANCE_LLM: String = "demo.prompt_enhance_llm"
}

/** Number of scenes the storyboard generator and storyboard-first short are fixed to. */
public const val STORYBOARD_SCENE_COUNT: Int = 3

/** Number of "X vs Y" pairs the comparison generator and comparison short are fixed to. */
public const val COMPARISON_PAIR_COUNT: Int = 4
