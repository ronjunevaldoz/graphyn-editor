package com.ronjunevaldoz.graphyn.plugins.preview

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType

/**
 * Media file output preview node.
 *
 * Displays file path and metadata (size, duration, codec) for media files.
 * Pass-through node: the file_path input is also output, so it can sit mid-pipeline.
 */
internal val specMediaFileOutput = NodeSpec(
    type = "media.file_output",
    label = "Media Output",
    category = CATEGORY_PREVIEW,
    description = "Display media file path and metadata (size, duration). Click 'Open' to view the file.",
    inputs = listOf(
        PortSpec("file_path", WorkflowType.StringType, description = "Path to output media file"),
    ),
    outputs = listOf(
        PortSpec("file_path", WorkflowType.StringType, description = "Pass-through file path"),
    ),
)
