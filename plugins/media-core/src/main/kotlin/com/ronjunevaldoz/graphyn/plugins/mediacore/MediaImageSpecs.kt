package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Phase 3 image-processing specs (resize, crop, sequence → video) plus an image collector. */
object MediaImageSpecs {
    val imageResize = NodeSpec(
        type = "media.image_resize",
        label = "Image Resize",
        description = "Scales an image to the given pixel dimensions.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
        outputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
        defaultValues = mapOf("width" to WorkflowValue.IntValue(1280), "height" to WorkflowValue.IntValue(720)),
    )

    val imageCrop = NodeSpec(
        type = "media.image_crop",
        label = "Image Crop",
        description = "Trims an image to a rectangular region.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("x", WorkflowType.IntType),
            PortSpec("y", WorkflowType.IntType),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
        outputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
        defaultValues = mapOf(
            "x" to WorkflowValue.IntValue(0),
            "y" to WorkflowValue.IntValue(0),
            "width" to WorkflowValue.IntValue(640),
            "height" to WorkflowValue.IntValue(480),
        ),
    )

    val imageFlip = NodeSpec(
        type = "media.image_flip",
        label = "Image Flip",
        description = "Mirrors an image horizontally — e.g. a left-pointing character becomes right-pointing without a second AI generation.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(PortSpec("image", MediaTypes.imageHandle)),
        outputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
    )

    val imagesList = NodeSpec(
        type = "media.images_list",
        label = "Images List",
        description = "Collects individual image handles into a list for the sequence encoder.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("image1", MediaTypes.imageHandle),
            PortSpec("image2", MediaTypes.imageHandle, required = false),
            PortSpec("image3", MediaTypes.imageHandle, required = false),
            PortSpec("image4", MediaTypes.imageHandle, required = false),
        ),
        outputs = listOf(PortSpec("images", WorkflowType.ListType(MediaTypes.imageHandle))),
    )

    val imageSequenceToVideo = NodeSpec(
        type = "media.image_sequence_to_video",
        label = "Image Sequence to Video",
        description = "Renders a list of images into an MP4 slideshow at the given frame rate.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("images", WorkflowType.ListType(MediaTypes.imageHandle)),
            PortSpec("fps", WorkflowType.DoubleType),
        ),
        outputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
            PortSpec("frame_count", WorkflowType.IntType),
        ),
        defaultValues = mapOf("fps" to WorkflowValue.DoubleValue(1.0)),
    )

    val kenBurns = NodeSpec(
        type = "media.ken_burns",
        label = "Ken Burns Pan/Zoom",
        description = "Renders a still image into a video clip with a slow pan/zoom, instead of a static repeat.",
        category = CATEGORY_MEDIA_VIDEO,
        inputs = listOf(
            PortSpec("image", MediaTypes.imageHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
            PortSpec("fps", WorkflowType.DoubleType),
            PortSpec("zoom_start", WorkflowType.DoubleType),
            PortSpec("zoom_end", WorkflowType.DoubleType),
            PortSpec("pan_x", WorkflowType.EnumType(listOf("center", "left_to_right", "right_to_left"))),
            PortSpec("pan_y", WorkflowType.EnumType(listOf("center", "top_to_bottom", "bottom_to_top"))),
            PortSpec("width", WorkflowType.IntType),
            PortSpec("height", WorkflowType.IntType),
        ),
        outputs = listOf(
            PortSpec("video", MediaTypes.videoHandle),
            PortSpec("duration_ms", WorkflowType.DoubleType),
        ),
        defaultValues = mapOf(
            "duration_ms" to WorkflowValue.DoubleValue(2000.0),
            "fps" to WorkflowValue.DoubleValue(24.0),
            "zoom_start" to WorkflowValue.DoubleValue(1.0),
            "zoom_end" to WorkflowValue.DoubleValue(1.15),
            "pan_x" to WorkflowValue.StringValue("center"),
            "pan_y" to WorkflowValue.StringValue("center"),
            "width" to WorkflowValue.IntValue(720),
            "height" to WorkflowValue.IntValue(1280),
        ),
    )

    val all = listOf(imageResize, imageCrop, imageFlip, imagesList, imageSequenceToVideo, kenBurns)
}
