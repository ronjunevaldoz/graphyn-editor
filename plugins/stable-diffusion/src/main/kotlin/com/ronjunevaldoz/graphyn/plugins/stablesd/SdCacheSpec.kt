package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.EnumType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.cache` — encapsulates `sd_cache_params_t`.
 *
 * Inference-step caching (EasyCache, TaylorSeer, Spectrum, etc.) to skip redundant DiT blocks.
 * All params except `mode`, `scm_mask`, and `scm_policy_dynamic` are passed to the CLI as a
 * single `--cache-option "key=val,key=val,..."` string. Defaults match `sd_cache_params_init()`.
 */
object SdCacheSpec {
    val cache = NodeSpec(
        type = "sd.cache",
        label = "SD Cache",
        description = "Inference-step caching for stable-diffusion.cpp (sd_cache_params_t). Reduces compute by skipping repetitive DiT blocks.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("mode", EnumType(SD_CACHE_MODES), portColor = COLOR_STRING,
                description = "CLI: --cache-mode <mode>. Caching algorithm. 'disabled' = off. All other params are ignored unless mode is set. Default: disabled."),
            PortSpec("reuse_threshold", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option threshold=<val>. Block reuse similarity threshold (EasyCache/UCache); or residual diff threshold (DBCache/TaylorSeer). Lower = more aggressive caching. Default: ∞."),
            PortSpec("start_percent", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option start=<val>. Denoising fraction [0,1] at which caching begins. Default: 0.15."),
            PortSpec("end_percent", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option end=<val>. Denoising fraction [0,1] at which caching ends. Default: 0.95."),
            PortSpec("error_decay_rate", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option decay=<val>. Decay rate applied to the accumulated cache-error estimate each step. Default: 1.0."),
            PortSpec("use_relative_threshold", BooleanType, portColor = COLOR_BOOL,
                description = "CLI: --cache-option relative=<true|false>. Treat reuse_threshold relative to block magnitude rather than absolute. Default: true."),
            PortSpec("reset_error_on_compute", BooleanType, portColor = COLOR_BOOL,
                description = "CLI: --cache-option reset=<true|false>. Reset error accumulation whenever a block is actually recomputed. Default: true."),
            PortSpec("fn_compute_blocks", IntType, portColor = COLOR_INT,
                description = "CLI: --cache-option Fn=<n>. Number of forward (encoder) blocks computed per step for EasyCache. Default: 8."),
            PortSpec("bn_compute_blocks", IntType, portColor = COLOR_INT,
                description = "CLI: --cache-option Bn=<n>. Number of backward (decoder) blocks computed per step for EasyCache. Default: 0."),
            PortSpec("residual_diff_threshold", DoubleType, portColor = COLOR_FLOAT,
                description = "DBCache/TaylorSeer residual diff threshold. Passed via --cache-option threshold=<val> (same key as reuse_threshold). Only relevant for dbcache/taylorseer modes. Default: 0.08."),
            PortSpec("max_warmup_steps", IntType, portColor = COLOR_INT,
                description = "CLI: --cache-option warmup=<n>. Steps before caching is activated (warmup period). Default: 8."),
            PortSpec("max_cached_steps", IntType, portColor = COLOR_INT,
                description = "Maximum number of steps that may be cached (-1 = unlimited). Stored in sd_cache_params_t; not exposed as a --cache-option key in this CLI version. Default: -1."),
            PortSpec("max_continuous_cached_steps", IntType, portColor = COLOR_INT,
                description = "Maximum consecutive cached steps before a forced recompute (-1 = unlimited). Stored in sd_cache_params_t; not exposed as a --cache-option key in this CLI version. Default: -1."),
            PortSpec("taylorseer_n_derivatives", IntType, portColor = COLOR_INT,
                description = "Number of Taylor series derivative orders for TaylorSeer. Stored in sd_cache_params_t; not exposed as a --cache-option key in this CLI version. Default: 1."),
            PortSpec("taylorseer_skip_interval", IntType, portColor = COLOR_INT,
                description = "Step interval between TaylorSeer recomputes. Stored in sd_cache_params_t; not exposed as a --cache-option key in this CLI version. Default: 1."),
            PortSpec("scm_mask", NullableType(StringType), portColor = COLOR_STRING,
                description = "CLI: --scm-mask <mask>. SCM block mask string (separate --scm-mask flag, NOT part of --cache-option). Null = auto policy."),
            PortSpec("scm_policy_dynamic", BooleanType, portColor = COLOR_BOOL,
                description = "CLI: false → --scm-policy static. When false, passes '--scm-policy static' (separate flag, NOT part of --cache-option). Default: true (dynamic)."),
            PortSpec("spectrum_w", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option w=<val>. Spectrum cache frequency weight. Only used when mode=spectrum. Default: 0.40."),
            PortSpec("spectrum_m", IntType, portColor = COLOR_INT,
                description = "CLI: --cache-option m=<n>. Spectrum cache M (number of cached spectra). Only used when mode=spectrum. Default: 3."),
            PortSpec("spectrum_lam", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option lam=<val>. Spectrum cache lambda regularisation. Only used when mode=spectrum. Default: 1.0."),
            PortSpec("spectrum_window_size", IntType, portColor = COLOR_INT,
                description = "CLI: --cache-option window=<n>. Spectrum cache sliding window size. Only used when mode=spectrum. Default: 2."),
            PortSpec("spectrum_flex_window", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option flex=<val>. Spectrum flexible window fraction [0,1]. Only used when mode=spectrum. Default: 0.50."),
            PortSpec("spectrum_warmup_steps", IntType, portColor = COLOR_INT,
                description = "CLI: --cache-option warmup=<n>. Spectrum-specific warmup steps (same 'warmup' key as max_warmup_steps; set only one). Only used when mode=spectrum. Default: 4."),
            PortSpec("spectrum_stop_percent", DoubleType, portColor = COLOR_FLOAT,
                description = "CLI: --cache-option stop=<val>. Denoising fraction [0,1] at which spectrum caching stops. Only used when mode=spectrum. Default: 0.9."),
        ),
        outputs = listOf(
            PortSpec("cache", OpaqueType, portColor = COLOR_SAMPLER,
                description = "Opaque cache config token passed to generation nodes (sd.txt2img, sd.img2img, sd.txt2vid, sd.img2vid)."),
        ),
        defaultValues = mapOf(
            "mode"                      to WorkflowValue.StringValue("disabled"),
            "reuse_threshold"           to WorkflowValue.DoubleValue(Double.MAX_VALUE),
            "start_percent"             to WorkflowValue.DoubleValue(0.15),
            "end_percent"               to WorkflowValue.DoubleValue(0.95),
            "error_decay_rate"          to WorkflowValue.DoubleValue(1.0),
            "use_relative_threshold"    to WorkflowValue.BooleanValue(true),
            "reset_error_on_compute"    to WorkflowValue.BooleanValue(true),
            "fn_compute_blocks"         to WorkflowValue.IntValue(8),
            "bn_compute_blocks"         to WorkflowValue.IntValue(0),
            "residual_diff_threshold"   to WorkflowValue.DoubleValue(0.08),
            "max_warmup_steps"          to WorkflowValue.IntValue(8),
            "max_cached_steps"          to WorkflowValue.IntValue(-1),
            "max_continuous_cached_steps" to WorkflowValue.IntValue(-1),
            "taylorseer_n_derivatives"  to WorkflowValue.IntValue(1),
            "taylorseer_skip_interval"  to WorkflowValue.IntValue(1),
            "scm_policy_dynamic"        to WorkflowValue.BooleanValue(true),
            "spectrum_w"                to WorkflowValue.DoubleValue(0.40),
            "spectrum_m"                to WorkflowValue.IntValue(3),
            "spectrum_lam"              to WorkflowValue.DoubleValue(1.0),
            "spectrum_window_size"      to WorkflowValue.IntValue(2),
            "spectrum_flex_window"      to WorkflowValue.DoubleValue(0.50),
            "spectrum_warmup_steps"     to WorkflowValue.IntValue(4),
            "spectrum_stop_percent"     to WorkflowValue.DoubleValue(0.9),
        ),
    )
}
