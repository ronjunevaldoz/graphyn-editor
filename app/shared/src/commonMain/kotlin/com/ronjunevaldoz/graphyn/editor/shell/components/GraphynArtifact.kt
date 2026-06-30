package com.ronjunevaldoz.graphyn.editor.shell.components

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal enum class ArtifactType(val label: String) {
    Image("IMAGE"), Video("VIDEO"), Audio("AUDIO"),
}

internal data class ArtifactItem(
    val nodeId: String,
    val nodeLabel: String,
    val portName: String,
    val filePath: String,
    val type: ArtifactType,
) {
    val fileName: String
        get() = filePath.substringAfterLast('/').substringAfterLast('\\').ifEmpty { filePath }
}

private val IMAGE_EXTS = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
private val VIDEO_EXTS = setOf("mp4", "mov", "mkv", "avi", "webm")
private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "ogg", "m4a")

internal fun extractArtifacts(
    result: WorkflowExecutionResult,
    nodeLabel: (String) -> String,
): List<ArtifactItem> = buildList {
    for ((nodeId, ports) in result.nodeOutputsByNodeId) {
        for ((portName, value) in ports) {
            val path = (value as? WorkflowValue.StringValue)?.value ?: continue
            val ext = path.substringAfterLast('.', "").lowercase()
            val type = when (ext) {
                in IMAGE_EXTS -> ArtifactType.Image
                in VIDEO_EXTS -> ArtifactType.Video
                in AUDIO_EXTS -> ArtifactType.Audio
                else -> continue
            }
            add(ArtifactItem(nodeId, nodeLabel(nodeId), portName, path, type))
        }
    }
}
