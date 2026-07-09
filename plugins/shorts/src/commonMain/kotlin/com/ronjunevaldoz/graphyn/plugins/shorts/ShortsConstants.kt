package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Shared configuration for the shorts pipeline. These are the vertical-video canvas dimensions and
 * the node-type strings this plugin references on other plugins' nodes by name. They are copied as
 * plain strings/ints (not typed imports) so this module stays dependency-free of the media-ai and
 * stable-diffusion plugins it composes — the execution engine resolves the types at runtime, the
 * same "reference by type string" pattern the sd.* / media.* wiring already uses.
 */
public object ShortsConstants {
    /** Vertical short canvas width in pixels (720×1280, i.e. 9:16). */
    public const val WIDTH: Int = 720

    /** Vertical short canvas height in pixels (720×1280, i.e. 9:16). */
    public const val HEIGHT: Int = 1280

    /** Node group/category the shorts specs register under. */
    public const val CATEGORY: String = "demo.composition"

    /**
     * `media.prompt_enhance` node type (JVM-only, lives in plugins/media-ai). Referenced by string
     * so this module does not depend on media-ai. Keep in sync with `MediaAiSpecs.promptEnhanceSpec`.
     */
    public const val PROMPT_ENHANCE_NODE_TYPE: String = "media.prompt_enhance"

    /**
     * `media.caption_style` node type (JVM-only, lives in plugins/media-ai). Referenced by string
     * so this module does not depend on media-ai. Keep in sync with `MediaAiSpecs.captionStyle`.
     */
    public const val CAPTION_STYLE_NODE_TYPE: String = "media.caption_style"

    /**
     * Default caption styling for the shorts pipeline. Keep in sync with `MediaAiSpecs.captionStyle`.
     *
     * "Arial" is not actually installed on most desktop targets (macOS ships Arial Hebrew/Arial
     * Narrow variants, not the plain family) — libass's font matching then silently substitutes a
     * mismatched fallback for the burned-in captions, which renders visibly worse than the
     * drawtext-based comparison-layout labels (different font-matching pipeline, confirmed fine with
     * the same nominal name). "Helvetica Neue" is a real installed family on macOS, renders cleanly
     * bold, and needs no substitution.
     */
    public val CAPTION_STYLE_DEFAULTS: Map<String, WorkflowValue> = mapOf(
        "font_family" to WorkflowValue.StringValue("Helvetica Neue"),
        "font_size" to WorkflowValue.IntValue(46),
        "text_color" to WorkflowValue.StringValue("#FFFFFF"),
        "background_color" to WorkflowValue.NullValue,
        "outline_color" to WorkflowValue.StringValue("#000000"),
        "outline_width" to WorkflowValue.IntValue(3),
        "shadow" to WorkflowValue.IntValue(1),
        "bold" to WorkflowValue.BooleanValue(true),
        "italic" to WorkflowValue.BooleanValue(false),
        "alignment" to WorkflowValue.StringValue("BottomCenter"),
        "margin_horizontal" to WorkflowValue.IntValue(40),
        "margin_vertical" to WorkflowValue.IntValue(60),
    )
}
