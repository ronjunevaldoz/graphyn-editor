package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Image edit pipeline (media Phase 3). Import an image, resize it, then crop a region.
 * Demonstrates the `image_resize` and `image_crop` nodes.
 */
internal val imageEditWorkflow = WorkflowDefinition(
    id = "image-edit", name = "Image Edit",
    nodes = listOf(
        guideNote(
            """
            Image Edit

            Resizes an image and crops a region out of it.

            Flow: Resolve Path → Image Import → Image Resize → Image Crop
            → Preview.
            Use cases: thumbnails, avatars, fixed-aspect exports.
            Tips: resize sets the working size; crop's x/y/width/height
            select the region to keep.
            """,
        ),
        NodeRef("resolveImage", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("sample.png"),
        )),
        NodeRef("import_image", "media.image_import"),
        NodeRef("resize", "media.image_resize", config = mapOf(
            "width" to WorkflowValue.IntValue(1280),
            "height" to WorkflowValue.IntValue(720),
        )),
        NodeRef("crop", "media.image_crop", config = mapOf(
            "x" to WorkflowValue.IntValue(140),
            "y" to WorkflowValue.IntValue(60),
            "width" to WorkflowValue.IntValue(1000),
            "height" to WorkflowValue.IntValue(600),
        )),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("resolveImage", "resolved_path", "import_image", "path"),
        ConnectionRef("import_image", "image", "resize", "image"),
        ConnectionRef("resize", "image", "crop", "image"),
        ConnectionRef("crop", "image", "preview", "value"),
    ),
)

/**
 * Slideshow (media Phase 3). Render a list of images into an MP4 at a fixed frame rate.
 * Demonstrates `images_list → image_sequence_to_video`.
 */
internal val slideshowWorkflow = WorkflowDefinition(
    id = "slideshow", name = "Slideshow",
    nodes = listOf(
        guideNote(
            """
            Slideshow

            Turns a set of images into a video at a fixed frame rate.

            Flow: Resolve Path ×N → Image Import ×N → Images List →
            Image Sequence to Video → Video Encode → Media Output.
            Use cases: photo reels, title cards, generated frames → clip.
            Tips: fps controls how long each frame shows; add more frames
            via image3/image4 on the Images List.
            """,
            height = 280,
        ),
        NodeRef("resolveFrame1", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("frame1.png"),
        )),
        NodeRef("resolveFrame2", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("frame2.png"),
        )),
        NodeRef("import1", "media.image_import"),
        NodeRef("import2", "media.image_import"),
        NodeRef("frames", "media.images_list"),
        NodeRef("sequence", "media.image_sequence_to_video", config = mapOf(
            "fps" to WorkflowValue.DoubleValue(1.0),
        )),
        NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to WorkflowValue.StringValue("slideshow.mp4"),
            "bitrate" to WorkflowValue.StringValue("high"),
        )),
        NodeRef("output", "media.file_output"),
    ),
    connections = listOf(
        ConnectionRef("resolveFrame1", "resolved_path", "import1", "path"),
        ConnectionRef("resolveFrame2", "resolved_path", "import2", "path"),
        ConnectionRef("import1", "image", "frames", "image1"),
        ConnectionRef("import2", "image", "frames", "image2"),
        ConnectionRef("frames", "images", "sequence", "images"),
        ConnectionRef("sequence", "video", "encode", "video"),
        ConnectionRef("encode", "file_path", "output", "file_path"),
    ),
)
