package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Document text extraction workflow (media Phase 2).
 *
 * Import an image and run OCR to read its text and bounding blocks.
 *
 * Demonstrates the `image_import → ocr` pairing. Like the other media-AI templates it is
 * structurally complete but needs `GRAPHYN_OCR_EXECUTABLE` configured (and the referenced image
 * present) to actually run.
 */
internal val documentOcrWorkflow = WorkflowDefinition(
    id = "document-ocr", name = "Document Text Extract",
    nodes = listOf(
        guideNote(
            """
            Document Text Extract

            Reads text out of an image with OCR.

            Flow: Resolve Path → Image Import → OCR → Preview.
            Use cases: receipts, scanned docs, screenshots, signage.
            Tips: OCR also returns per-block bounding boxes + confidence;
            wire the blocks output into a script for layout-aware parsing.
            """,
        ),
        NodeRef("resolveImage", "io.resolve_path", config = mapOf(
            "base_dir" to WorkflowValue.StringValue(MEDIA_DIR),
            "relative_path" to WorkflowValue.StringValue("sample.png"),
        )),
        NodeRef("import_image", "media.image_import"),
        NodeRef("ocr", "media.ocr", config = mapOf(
            "language" to WorkflowValue.StringValue("en"),
        )),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("resolveImage", "resolved_path", "import_image", "path"),
        ConnectionRef("import_image", "image", "ocr", "image"),
        ConnectionRef("ocr", "text", "preview", "value"),
    ),
)
