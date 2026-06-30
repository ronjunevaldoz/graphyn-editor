package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.ListType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.id_cond` — reference image, PhotoMaker, and PuLID id-conditioning config.
 *
 * Outputs an opaque token consumed by generation nodes (sd.txt2img, sd.img2img).
 * Only wire this node when using reference images, PhotoMaker, or PuLID — it's optional.
 */
object SdIdCondSpec {
    val idCond = NodeSpec(
        type = "sd.id_cond",
        label = "SD ID Conditioning",
        description = "Reference images + PhotoMaker/PuLID id-conditioning. Wire id_cond → generation node.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("ref_images", NullableType(ListType(StringType)), portColor = COLOR_IMAGE,
                description = "--ref-image: Paths to reference images (PhotoMaker/PuLID/Qwen). Null = none."),
            PortSpec("auto_resize_ref_image", BooleanType, portColor = COLOR_BOOL,
                description = "--disable-auto-resize-ref-image: Auto-resize reference images to width/height. Default: true."),
            PortSpec("increase_ref_index", BooleanType, portColor = COLOR_BOOL,
                description = "--increase-ref-index: Increment reference image index per batch item."),
            PortSpec("pm_id_embed_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--pm-id-embed-path: PhotoMaker id-embedding file path."),
            PortSpec("pm_id_images_dir", NullableType(StringType), portColor = COLOR_STRING,
                description = "--pm-id-images-dir: PhotoMaker id-images directory (alternative to embed path)."),
            PortSpec("pm_style_strength", DoubleType, portColor = COLOR_FLOAT,
                description = "--pm-style-strength: PhotoMaker style strength. Default: 20.0."),
            PortSpec("pulid_id_embedding_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--pulid-id-embedding: PuLID id-embedding file path."),
            PortSpec("pulid_id_weight", DoubleType, portColor = COLOR_FLOAT,
                description = "--pulid-id-weight: PuLID id-embedding weight. Default: 1.0."),
        ),
        outputs = listOf(
            PortSpec("id_cond", OpaqueType, portColor = COLOR_ID_COND,
                description = "Opaque id-conditioning token consumed by generation nodes."),
        ),
        defaultValues = mapOf(
            "auto_resize_ref_image" to WorkflowValue.BooleanValue(true),
            "increase_ref_index"    to WorkflowValue.BooleanValue(false),
            "pm_style_strength"     to WorkflowValue.DoubleValue(20.0),
            "pulid_id_weight"       to WorkflowValue.DoubleValue(1.0),
        ),
    )
}
