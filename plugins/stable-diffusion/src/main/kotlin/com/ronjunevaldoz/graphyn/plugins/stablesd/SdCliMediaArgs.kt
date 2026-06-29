package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue

/** Builds `--hires` and related args from an `sd.hires` record. */
internal fun buildHiresArgs(inputs: Map<String, WorkflowValue>): List<String> = buildList {
    if (inputs.bool("enabled") != true) return@buildList
    add("--hires")
    inputs.str("upscaler")?.let { add("--hires-upscaler"); add(it) }
    inputs.str("model_path")?.let { add("--hires-upscalers-dir"); add(it) }
    inputs.double("scale")?.let { add("--hires-scale"); add(it.toString()) }
    inputs.int("target_width")?.takeIf { it > 0 }?.let { add("--hires-width"); add(it.toString()) }
    inputs.int("target_height")?.takeIf { it > 0 }?.let { add("--hires-height"); add(it.toString()) }
    inputs.int("steps")?.takeIf { it > 0 }?.let { add("--hires-steps"); add(it.toString()) }
    inputs.double("denoising_strength")?.let { add("--hires-denoising-strength"); add(it.toString()) }
    inputs.int("upscale_tile_size")?.let { add("--hires-upscale-tile-size"); add(it.toString()) }
    inputs["custom_sigmas"]?.asList()?.let { s ->
        add("--hires-sigmas"); add(s.joinToString(",") { (it as? DoubleValue)?.value?.toString() ?: it.toString() })
    }
}

/**
 * Builds `--cache-mode` and `--cache-option key=val,...` args from an `sd.cache` record.
 * Individual cache params (reuse_threshold, start_percent, etc.) are merged into a single
 * `--cache-option` string per the stable-diffusion.cpp CLI contract.
 */
internal fun buildCacheArgs(inputs: Map<String, WorkflowValue>): List<String> = buildList {
    val mode = inputs.str("mode") ?: return@buildList
    if (mode == "disabled") return@buildList
    add("--cache-mode"); add(mode)
    val opts = buildList<String> {
        inputs.double("reuse_threshold")?.let { add("threshold=$it") }
        inputs.double("start_percent")?.let { add("start=$it") }
        inputs.double("end_percent")?.let { add("end=$it") }
        inputs.double("error_decay_rate")?.let { add("decay=$it") }
        inputs.bool("use_relative_threshold")?.let { add("relative=$it") }
        inputs.bool("reset_error_on_compute")?.let { add("reset=$it") }
        inputs.int("fn_compute_blocks")?.let { add("Fn=$it") }
        inputs.int("bn_compute_blocks")?.let { add("Bn=$it") }
        inputs.int("max_warmup_steps")?.let { add("warmup=$it") }
        inputs.double("spectrum_w")?.let { add("w=$it") }
        inputs.int("spectrum_m")?.let { add("m=$it") }
        inputs.double("spectrum_lam")?.let { add("lam=$it") }
        inputs.int("spectrum_window_size")?.let { add("window=$it") }
        inputs.double("spectrum_flex_window")?.let { add("flex=$it") }
        inputs.int("spectrum_warmup_steps")?.let { add("warmup=$it") }
        inputs.double("spectrum_stop_percent")?.let { add("stop=$it") }
    }
    if (opts.isNotEmpty()) { add("--cache-option"); add(opts.joinToString(",")) }
    inputs.str("scm_mask")?.let { add("--scm-mask"); add(it) }
    if (inputs.bool("scm_policy_dynamic") == false) { add("--scm-policy"); add("static") }
}

/** Builds `--vae-tiling` and related args from an `sd.vae_tiling` record. */
internal fun buildTilingArgs(inputs: Map<String, WorkflowValue>): List<String> = buildList {
    if (inputs.bool("enabled") != true) return@buildList
    add("--vae-tiling")
    if (inputs.bool("temporal_tiling") == true) add("--temporal-tiling")
    inputs.int("tile_size_x")?.takeIf { it > 0 }?.let { add("--vae-tile-size"); add(it.toString()) }
    inputs.double("target_overlap")?.let { add("--vae-tile-overlap"); add(it.toString()) }
    inputs.double("rel_size_x")?.takeIf { it > 0 }?.let { add("--vae-relative-tile-size"); add(it.toString()) }
    inputs.str("extra_tiling_args")?.let { add("--extra-tiling-args"); add(it) }
}
