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
}

/** Number of scenes the storyboard generator and storyboard-first short are fixed to. */
public const val STORYBOARD_SCENE_COUNT: Int = 3
