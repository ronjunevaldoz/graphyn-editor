package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.booleanValue as b
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

/**
 * Edits a single existing scene image via FLUX Kontext, conditioning on [referenceImagePath] (a
 * previously-saved scene keyframe — see `imageMotionSceneSubgraphDynamic`'s
 * `imagePathSidecarPath`) instead of generating from scratch. [editInstruction] is a Kontext
 * instruction ("change the shirt to red"), not a full scene description — see
 * DemoFluxKontextImg2ImgDef.kt and docs/architecture/lessons.md for why `ref_images` (not
 * `sd.img2img`'s `init_image`) is required for real conditioning. Unlike
 * `imageMotionSceneSubgraphInternal`, this skips `promptEnhance`'s niche/visual_style/character
 * enrichment entirely — an edit instruction should not be decorated the way a full scene
 * description is.
 */
public fun imageMotionSceneEditSubgraph(
    id: String,
    referenceImagePath: String,
    editInstruction: String,
    imageCount: Int = 2,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    imagePathSidecarPath: String? = null,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = "Image Motion Scene (Edit)",
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(FLUX_KONTEXT_DIFFUSION))))
        add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(FLUX_CLIP_L), "t5xxl_path" to s(FLUX_T5XXL))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(FLUX_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf(
            "diffusion_flash_attn" to b(true),
            "n_threads" to i(-1),
            // Confirmed necessary for Kontext in DemoFluxKontextImg2ImgDef.kt.
            "clip_on_cpu" to b(true),
        )))
        add(NodeRef("idCond", "sd.id_cond", config = mapOf(
            "ref_images" to WorkflowValue.ListValue(listOf(s(referenceImagePath))),
        )))
        add(NodeRef("sampler", "sd.sampler", config = mapOf(
            "sample_method" to s("euler"),
            "scheduler" to s("discrete"),
            "sample_steps" to i(20),
            "txt_cfg" to d(1.0),
            "distilled_guidance" to d(3.5),
            "flow_shift" to d(3.0),
        )))
        add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
            "prompt" to s(editInstruction),
            "negative_prompt" to s(""),
            "width" to i(width),
            "height" to i(height),
            "seed" to i(-1),
            "batch_count" to i(1),
        )))
        if (imagePathSidecarPath != null) {
            // Persists the edited image's path so a later edit conditions on THIS result, not
            // the pre-edit image that was passed in as referenceImagePath — see
            // imageMotionSceneSubgraphInternal's identical sidecar for the same reasoning.
            add(NodeRef("imagePathSave", "io.file_write", config = mapOf(
                "path" to s(imagePathSidecarPath), "append" to WorkflowValue.BooleanValue(false),
            )))
        }
        add(NodeRef("import", "media.image_import"))
        add(NodeRef("kenBurns", "media.ken_burns", config = mapOf(
            "duration_ms" to d(imageCount * 1000.0),
            "fps" to d(24.0),
            "zoom_start" to d(1.0),
            "zoom_end" to d(1.15),
            "pan_x" to s("center"),
            "pan_y" to s("center"),
            "width" to i(width),
            "height" to i(height),
        )))
        add(NodeRef("preview", "preview.view"))
    },
    connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("encoders", "encoders", "model", "encoders"))
        add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "ctx", "model"))
        add(ConnectionRef("ctx", "context", "txt2img", "context"))
        add(ConnectionRef("sampler", "sampler", "txt2img", "sampler"))
        add(ConnectionRef("idCond", "id_cond", "txt2img", "id_cond"))
        if (imagePathSidecarPath != null) add(ConnectionRef("txt2img", "image", "imagePathSave", "content"))
        add(ConnectionRef("txt2img", "image", "import", "path"))
        add(ConnectionRef("import", "image", "kenBurns", "image"))
        add(ConnectionRef("kenBurns", "video", "preview", "value"))
    },
)
