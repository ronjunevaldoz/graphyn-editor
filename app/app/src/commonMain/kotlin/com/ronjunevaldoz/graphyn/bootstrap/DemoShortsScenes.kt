package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediaai.promptEnhanceSpec

private const val FLUX_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
private const val FLUX_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
private const val FLUX_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val FLUX_VAE = "/models/flux/vae/ae.safetensors"
private const val WAN5B_DIFFUSION = "/models/wan/Wan2.2-TI2V-5B-Q4_K_M.gguf"
private const val WAN5B_T5 = "/models/wan/umt5-xxl-encoder-Q5_K_M.gguf"
private const val WAN5B_CLIP_VISION = "/models/wan/clip_vision_h.safetensors"
private const val WAN5B_VAE = "/models/wan/Wan2.2_VAE.safetensors"
private const val WIDE = SHORTS_WIDTH
private const val TALL = SHORTS_HEIGHT
private const val FRAMES = SHORTS_FRAME_COUNT
private const val FPS = SHORTS_FPS

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)
private fun b(value: Boolean) = WorkflowValue.BooleanValue(value)

internal fun imageSceneSubgraph(index: Int) = sceneSubgraph(index, useImageMotion = true)

internal fun videoSceneSubgraph(index: Int) = sceneSubgraph(index, useImageMotion = false)

internal fun stitchBatchSubgraph(index: Int) = WorkflowDefinition(
    id = "stitch-batch-$index",
    name = "Batch $index",
    nodePositions = shortsBatchNodePositions(),
    nodes = listOf(
        NodeRef("clips", "media.videos_list"),
        NodeRef("stitch", "media.video_stitch", config = mapOf("transition" to s("cut"))),
    ),
    connections = listOf(
        ConnectionRef("clips", "videos", "stitch", "videos"),
    ),
)

private fun sceneSubgraph(index: Int, useImageMotion: Boolean) = WorkflowDefinition(
    id = if (useImageMotion) "image-scene-$index" else "video-scene-$index",
    name = if (useImageMotion) "Image Scene $index" else "Video Scene $index",
    nodePositions = shortsSceneNodePositions(useImageMotion),
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf(
            "diffusion_model_path" to s(if (useImageMotion) FLUX_DIFFUSION else WAN5B_DIFFUSION),
        )))
        add(NodeRef("encoders", "sd.encoders", config = if (useImageMotion) {
            mapOf("clip_l_path" to s(FLUX_CLIP_L), "t5xxl_path" to s(FLUX_T5XXL))
        } else {
            mapOf("t5xxl_path" to s(WAN5B_T5), "clip_vision_path" to s(WAN5B_CLIP_VISION))
        }))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(if (useImageMotion) FLUX_VAE else WAN5B_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf("diffusion_flash_attn" to b(true), "n_threads" to i(-1))))
        if (useImageMotion) {
            add(NodeRef("promptEnhance", promptEnhanceSpec.type))
            add(NodeRef("sampler", "sd.sampler", config = mapOf(
                "sample_method" to s("euler"),
                "scheduler" to s("discrete"),
                "sample_steps" to i(4),
                "txt_cfg" to d(1.0),
                "distilled_guidance" to d(3.5),
                "flow_shift" to d(3.0),
            )))
            add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
                "prompt" to s("a cinematic scene, gentle camera push-in, filmic lighting, highly detailed"),
                "negative_prompt" to s(""),
                "width" to i(WIDE),
                "height" to i(TALL),
                "seed" to i(-1),
                "batch_count" to i(1),
            )))
            add(NodeRef("import", "media.image_import"))
            add(NodeRef("frames", "media.images_list"))
            add(NodeRef("sequence", "media.image_sequence_to_video", config = mapOf("fps" to d(1.0))))
        } else {
            add(NodeRef("promptEnhance", promptEnhanceSpec.type))
            add(NodeRef("sampler", "sd.sampler", config = mapOf(
                "sample_method" to s("euler"),
                "scheduler" to s("discrete"),
                "sample_steps" to i(20),
                "txt_cfg" to d(5.0),
                "flow_shift" to d(5.0),
            )))
            add(NodeRef("txt2vid", "sd.txt2vid", config = mapOf(
                "negative_prompt" to s(""),
                "width" to i(WIDE),
                "height" to i(TALL),
                "video_frames" to i(FRAMES),
                "fps" to i(FPS),
                "seed" to i(-1),
            )))
            add(NodeRef("wrap", "script.eval", config = mapOf("code" to s(SHORTS_WRAP_FRAMES_SCRIPT))))
            add(NodeRef("sequence", "media.image_sequence_to_video", config = mapOf("fps" to d(FPS.toDouble()))))
        }
        add(NodeRef("preview", "preview.view"))
    },
    connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("encoders", "encoders", "model", "encoders"))
        add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "ctx", "model"))
        if (useImageMotion) {
            add(ConnectionRef("promptEnhance", "prompt", "txt2img", "prompt"))
            add(ConnectionRef("promptEnhance", "negative_prompt", "txt2img", "negative_prompt"))
            add(ConnectionRef("ctx", "context", "txt2img", "context"))
            add(ConnectionRef("sampler", "sampler", "txt2img", "sampler"))
            add(ConnectionRef("txt2img", "image", "import", "path"))
            add(ConnectionRef("import", "image", "frames", "image1"))
            add(ConnectionRef("import", "image", "frames", "image2"))
            add(ConnectionRef("import", "image", "frames", "image3"))
            add(ConnectionRef("import", "image", "frames", "image4"))
        } else {
            add(ConnectionRef("promptEnhance", "prompt", "txt2vid", "prompt"))
            add(ConnectionRef("promptEnhance", "negative_prompt", "txt2vid", "negative_prompt"))
            add(ConnectionRef("ctx", "context", "txt2vid", "context"))
            add(ConnectionRef("sampler", "sampler", "txt2vid", "sampler"))
            add(ConnectionRef("txt2vid", "frames", "wrap", "input"))
        }
        if (useImageMotion) {
            add(ConnectionRef("frames", "images", "sequence", "images"))
        } else {
            add(ConnectionRef("wrap", "result", "sequence", "images"))
        }
        add(ConnectionRef("sequence", "video", "preview", "value"))
    },
)
